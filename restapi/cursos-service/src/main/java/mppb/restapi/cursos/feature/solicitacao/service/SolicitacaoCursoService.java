package mppb.restapi.cursos.feature.solicitacao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualMetadataDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoResponseDto;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import mppb.restapi.common.infrastructure.service.mpvirtual.MPVirtualIntegrationService;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.cursos.feature.cadastro.repository.EntidadeTutoraRepository;
import mppb.restapi.cursos.feature.common.repository.ServidorRepository;
import mppb.restapi.cursos.feature.curso.dto.AssociarMinistranteDto;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.lookup.entities.*;
import mppb.restapi.cursos.feature.lookup.repository.*;
import mppb.restapi.cursos.feature.ministrante.repository.MinistranteRepository;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoRequestDto;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoResponseDto;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoAgenda;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoCurso;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoMinistrante;
import mppb.restapi.cursos.feature.solicitacao.repository.SolicitacaoCursoRepository;
import mppb.restapi.cursos.feature.solicitacao.specification.SolicitacaoCursoSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitacaoCursoService {

    private final SolicitacaoCursoRepository solicitacaoRepository;
    private final SolicitacaoReportService reportService;
    private final MPVirtualIntegrationService mpVirtualService;
    private final SolicitacaoCursoSpecification solicitacaoSpecification;

    // Lookups
    private final ModalidadeRepository modalidadeRepository;
    private final PublicoAlvoRepository publicoAlvoRepository;
    private final LocalEventoRepository localEventoRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final StatusTramitacaoRepository statusTramitacaoRepository;
    private final PapelMinistranteRepository papelMinistranteRepository;
    private final MinistranteRepository ministranteRepositoryRef;
    private final CursoService cursoService;
    private final ServidorRepository servidorRepository;
    private final EntidadeTutoraRepository entidadeTutoraRepository;

    /**
     * Fluxo de criação de solicitação:
     * 1. Salva entidade com status INICIAL ou SUBMETIDO.
     * 2. O protocolo no MPVirtual NÃO ocorre aqui, deve ser disparado manualmente
     * pelo CEAF.
     * 
     * @param dto Os dados da solicitação preenchidos pelo usuário
     */
    @Transactional
    public SolicitacaoCursoResponseDto solicitarCurso(SolicitacaoCursoRequestDto dto) {
        log.info("Processando nova solicitação de curso: {}", dto.getNomeCurso());

        // 1. Mapear DTO para Entidade
        SolicitacaoCurso solicitacao = mapToEntity(dto);

        // Garante que o número do processo esteja nulo na criação inicial
        solicitacao.setNumeroPgaMpVirtual(null);

        // Identificação do Interessado via Token de Autênticação e mapeamento do RH
        Long idLogado = getIdServidorLogado();
        if (idLogado != null) {
            solicitacao.setIdServidorInteressado(idLogado);
            servidorRepository.buscarServidorPorId(idLogado.intValue()).ifPresent(servidor -> {
                solicitacao.setNomeSolicitante(servidor.getNome());
            });
        }
        solicitacao.setIdOrgaoInteressado(1L); // Mock ou extrair do token se disponível

        // Se for RASCUNHO, usamos status 1. Se SUBMETIDO, usamos 2 (Em Análise)
        validarMinistrantesSeSubmetido(dto);
        Long idStatus = "SUBMETIDO".equalsIgnoreCase(dto.getStatus()) ? 2L : 1L;

        StatusTramitacao status = statusTramitacaoRepository.findById(idStatus)
                .orElseThrow(() -> new AppException("Status de tramitação " + idStatus + " não encontrado."));
        solicitacao.setStatus(status);

        // Salva a entidade primeiro para ter o ID
        SolicitacaoCurso solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        return mapToResponseDto(solicitacaoSalva);
    }

    private MPVirtualProcessoResponseDto gerarEEnviarParaMpVirtual(SolicitacaoCurso solicitacaoSalva) {
        // 2. Gerar PDF do PGA
        byte[] pdfGerado = reportService.gerarPdfSolicitacao(solicitacaoSalva);

        // 3. Enviar para o MPVirtual
        try {
            // O requerente e o interessado no MPVirtual deve ser o usuário que solicitou o
            // curso inicialmente
            Integer idPessoaRequestor = null;
            Long idServidor = solicitacaoSalva.getIdServidorInteressado();

            if (idServidor != null) {
                idPessoaRequestor = servidorRepository.buscarServidorPorId(idServidor.intValue())
                        .map(mppb.restapi.cursos.feature.common.projection.ServidorProjection::getIdPessoa)
                        .orElse(null);
            }

            // Fallback removido. Deve-se garantir que o servidor interessado tenha um
            // idPessoa no cadastro de pessoal.
            if (idPessoaRequestor == null) {
                throw new BusinessRuleException(
                        "Não foi possível identificar o ID da Pessoa Física para o servidor interessado (ID: "
                                + idServidor + "). Por favor, verifique o cadastro do servidor.");
            }

            // A responsabilidade de tramitação agora é do CEAF (ID 16)
            Long idOrgaoCeaf = 16L;

            MPVirtualIntegrationService.ProtocoloResult result = mpVirtualService.abrirProcessoCurso(
                    "Solicitação de Curso: " + solicitacaoSalva.getNomeCurso(),
                    extrairResumo(solicitacaoSalva),
                    idOrgaoCeaf,
                    idPessoaRequestor,
                    pdfGerado);

            MPVirtualProcessoResponseDto integracaoResponse = result.response();

            // 4. Recebido o número do processo, atualiza e muda o status para PROTOCOLADO
            if (integracaoResponse != null && integracaoResponse.getNumeroProcesso() != null) {
                solicitacaoSalva.setNumeroPgaMpVirtual(integracaoResponse.getNumeroProcesso());
                solicitacaoSalva.setIdPgaMpVirtual(integracaoResponse.getIdProcesso());
                statusTramitacaoRepository.findById(3L).ifPresent(solicitacaoSalva::setStatus);

                solicitacaoRepository.save(solicitacaoSalva);
                log.info("Solicitação {} protocolada no MPVirtual com número {}", solicitacaoSalva.getId(),
                        integracaoResponse.getNumeroProcesso());
            } else {
                throw new AppException("A API do MPVirtual não retornou um número de processo válido.");
            }

            return integracaoResponse;
        } catch (Exception e) {
            log.error("Erro no fluxo do MPVirtual.", e);
            throw new AppException(e, "Erro ao abrir processo no MPVirtual: " + e.getMessage());
        }
    }

    private SolicitacaoCurso mapToEntity(SolicitacaoCursoRequestDto dto) {
        SolicitacaoCurso solicitacao = new SolicitacaoCurso();

        solicitacao.setNomeCurso(dto.getNomeCurso());
        solicitacao.setEstimativaParticipantes(dto.getEstimativaParticipantes());

        // Identificação - Removido Mock fixo para ser setado no service principal
        // solicitacao.setIdServidorInteressado(64601L);
        // solicitacao.setIdOrgaoInteressado(1L);

        // Lookups
        if (dto.getIdModalidade() != null) {
            Modalidade modalidade = modalidadeRepository.findById(dto.getIdModalidade())
                    .orElseThrow(() -> new NotFoundException("Modalidade não encontrada"));
            solicitacao.setModalidade(modalidade);
        }

        if (dto.getIdTipoEvento() != null) {
            TipoEvento tipoEvento = tipoEventoRepository.findById(dto.getIdTipoEvento())
                    .orElseThrow(() -> new NotFoundException("Tipo de Evento não encontrado"));
            solicitacao.setTipoEvento(tipoEvento);
        }

        if (dto.getIdsPublicoAlvo() != null && !dto.getIdsPublicoAlvo().isEmpty()) {
            List<PublicoAlvo> publicosAlvo = publicoAlvoRepository.findAllById(dto.getIdsPublicoAlvo());
            if (publicosAlvo.isEmpty()) {
                throw new NotFoundException("Nenhum público-alvo encontrado para os IDs fornecidos");
            }
            solicitacao.setPublicosAlvo(publicosAlvo);
        }

        if (dto.getIdLocalEvento() != null) {
            LocalEvento local = localEventoRepository.findById(dto.getIdLocalEvento())
                    .orElseThrow(() -> new NotFoundException("Local do evento não encontrado"));
            solicitacao.setLocalEvento(local);
        }

        if (dto.getIdEntidadeTutora() != null) {
            EntidadeTutora tutor = entidadeTutoraRepository.findById(dto.getIdEntidadeTutora())
                    .orElseThrow(() -> new NotFoundException("Entidade Tutora não encontrada"));
            solicitacao.setEntidadeTutora(tutor);
        }

        solicitacao.setDescricaoLocalExterno(dto.getDescricaoLocalExterno());
        solicitacao.setTemOrgaoExterno(dto.getTemOrgaoExterno());
        solicitacao.setNomeOrgaoExterno(dto.getNomeOrgaoExterno());

        // Apoio Logístico
        solicitacao.setCardDivulgacao(dto.getCardDivulgacao());
        solicitacao.setComunicacaoImprensa(dto.getComunicacaoImprensa());
        solicitacao.setOficioConvite(dto.getOficioConvite());
        solicitacao.setCoffeeBreak(dto.getCoffeeBreak());
        solicitacao.setKitParticipante(dto.getKitParticipante());
        solicitacao.setProgramacaoImpressa(dto.getProgramacaoImpressa());
        solicitacao.setTrasladoMinistrante(dto.getTrasladoMinistrante());

        // Financeiro
        solicitacao.setPagamentoHonorario(dto.getPagamentoHonorario());
        solicitacao.setPassagemAerea(dto.getPassagemAerea());
        solicitacao.setHospedagem(dto.getHospedagem());

        solicitacao.setObservacoesGerais(dto.getObservacoesGerais());
        solicitacao.setIdArquivoProgramacaoGed(dto.getIdArquivoProgramacaoGed());

        // Filhos: Agendas
        if (dto.getAgendas() != null) {
            dto.getAgendas().forEach(agendaDto -> {
                SolicitacaoAgenda agenda = new SolicitacaoAgenda();
                agenda.setDataInicio(agendaDto.getDataInicio());
                agenda.setDataFim(agendaDto.getDataFim());
                agenda.setHorarioInicio(agendaDto.getHorarioInicio());
                agenda.setHorarioFim(agendaDto.getHorarioFim());
                solicitacao.addAgenda(agenda);
            });
        }

        // Filhos: Ministrantes
        if (dto.getMinistrantes() != null) {
            dto.getMinistrantes().forEach(minDto -> {
                SolicitacaoMinistrante ministrante = new SolicitacaoMinistrante();
                ministrante.setIdServidor(minDto.getIdServidor());
                ministrante.setNomeMinistrante(minDto.getNomeMinistrante());
                ministrante.setTelefone(minDto.getTelefone());
                String cleanCpf = minDto.getCpf() != null ? minDto.getCpf().replaceAll("\\D", "") : null;
                ministrante.setCpf(cleanCpf);
                ministrante.setEmail(minDto.getEmail());
                ministrante.setIdCurriculo(minDto.getIdCurriculo());
                ministrante.setIdFoto(minDto.getIdFoto());

                if (minDto.getMinistranteId() != null) {
                    ministranteRepositoryRef.findById(minDto.getMinistranteId())
                            .ifPresent(ministrante::setMinistrante);
                }

                PapelMinistrante papel = papelMinistranteRepository.findById(minDto.getIdPapelMinistrante())
                        .orElseThrow(() -> new NotFoundException("Papel de ministrante não encontrado"));
                ministrante.setPapelMinistrante(papel);

                solicitacao.addMinistrante(ministrante);
            });
        }

        return solicitacao;
    }

    private SolicitacaoCursoResponseDto mapToResponseDto(SolicitacaoCurso entity) {
        SolicitacaoCursoResponseDto dto = new SolicitacaoCursoResponseDto();
        dto.setId(entity.getId());
        dto.setNumeroPgaMpVirtual(entity.getNumeroPgaMpVirtual());
        dto.setUuidReferencia(entity.getUuidReferencia());
        dto.setNomeCurso(entity.getNomeCurso());
        dto.setEstimativaParticipantes(entity.getEstimativaParticipantes());

        if (entity.getNomeSolicitante() != null) {
            dto.setNomeSolicitante(entity.getNomeSolicitante());
        } else if (entity.getIdServidorInteressado() != null) {
            servidorRepository.buscarServidorPorId(entity.getIdServidorInteressado().intValue())
                    .ifPresentOrElse(
                            servidor -> dto.setNomeSolicitante(servidor.getNome()),
                            () -> {
                                dto.setNomeSolicitante("Solicitante não identificado");
                            });
        }

        if (entity.getStatus() != null) {
            dto.setStatusDescricao(entity.getStatus().getDescricao());
        }
        if (entity.getModalidade() != null) {
            dto.setIdModalidade(entity.getModalidade().getId());
            dto.setModalidadeDescricao(entity.getModalidade().getDescricao());
        }
        if (entity.getTipoEvento() != null) {
            dto.setIdTipoEvento(entity.getTipoEvento().getId());
            dto.setTipoEventoDescricao(entity.getTipoEvento().getDescricao());
        }
        if (entity.getPublicosAlvo() != null && !entity.getPublicosAlvo().isEmpty()) {
            dto.setIdsPublicoAlvo(entity.getPublicosAlvo().stream().map(PublicoAlvo::getId).toList());
            dto.setPublicosAlvoDescricao(entity.getPublicosAlvo().stream().map(PublicoAlvo::getDescricao).toList());
        }
        if (entity.getLocalEvento() != null) {
            dto.setIdLocalEvento(entity.getLocalEvento().getId());
            dto.setLocalEventoDescricao(entity.getLocalEvento().getNome());
        }

        if (entity.getEntidadeTutora() != null) {
            dto.setIdEntidadeTutora(entity.getEntidadeTutora().getId());
            dto.setEntidadeTutoraNome(entity.getEntidadeTutora().getNome());
        }

        // Campos de texto e booleanos
        dto.setDescricaoLocalExterno(entity.getDescricaoLocalExterno());
        dto.setTemOrgaoExterno(entity.getTemOrgaoExterno());
        dto.setNomeOrgaoExterno(entity.getNomeOrgaoExterno());
        dto.setCardDivulgacao(entity.getCardDivulgacao());
        dto.setComunicacaoImprensa(entity.getComunicacaoImprensa());
        dto.setOficioConvite(entity.getOficioConvite());
        dto.setCoffeeBreak(entity.getCoffeeBreak());
        dto.setKitParticipante(entity.getKitParticipante());
        dto.setProgramacaoImpressa(entity.getProgramacaoImpressa());
        dto.setTrasladoMinistrante(entity.getTrasladoMinistrante());
        dto.setPagamentoHonorario(entity.getPagamentoHonorario());
        dto.setPassagemAerea(entity.getPassagemAerea());
        dto.setHospedagem(entity.getHospedagem());
        dto.setObservacoesGerais(entity.getObservacoesGerais());
        dto.setIdArquivoProgramacaoGed(entity.getIdArquivoProgramacaoGed());

        // Agendas
        if (entity.getAgendas() != null) {
            dto.setAgendas(entity.getAgendas().stream().map(a -> {
                SolicitacaoCursoResponseDto.AgendaResponseDto ad = new SolicitacaoCursoResponseDto.AgendaResponseDto();
                ad.setId(a.getId());
                ad.setDataInicio(a.getDataInicio());
                ad.setDataFim(a.getDataFim());
                ad.setHorarioInicio(a.getHorarioInicio());
                ad.setHorarioFim(a.getHorarioFim());
                return ad;
            }).toList());
        }

        // Ministrantes
        if (entity.getMinistrantes() != null) {
            dto.setMinistrantes(entity.getMinistrantes().stream().map(m -> {
                SolicitacaoCursoResponseDto.MinistranteResponseDto md = new SolicitacaoCursoResponseDto.MinistranteResponseDto();
                md.setId(m.getId());
                if (m.getPapelMinistrante() != null)
                    md.setIdPapelMinistrante(m.getPapelMinistrante().getId());
                if (m.getMinistrante() != null)
                    md.setMinistranteId(m.getMinistrante().getId());
                md.setIdServidor(m.getIdServidor());
                md.setNomeMinistrante(m.getNomeMinistrante());
                md.setTelefone(m.getTelefone());
                md.setCpf(m.getCpf());
                md.setEmail(m.getEmail());
                md.setIdFoto(m.getIdFoto());
                md.setIdCurriculo(m.getIdCurriculo());
                return md;
            }).toList());
        }

        dto.setIdCursoGerado(entity.getIdCursoGerado());
        dto.setIdPgaMpVirtual(entity.getIdPgaMpVirtual());
        dto.setIdMinuta(entity.getIdMinuta());
        dto.setDataCriacao(entity.getDataCriacao());
        dto.setDataModificacao(entity.getDataModificacao());

        return dto;
    }

    public SolicitacaoCursoResponseDto buscarPorId(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        validarPropriedadeOuAdmin(solicitacao);

        return mapToResponseDto(solicitacao);
    }

    public Page<SolicitacaoCursoResponseDto> buscarPaginado(String search, Long statusId, Long minStatusId,
            Pageable pageable) {
        boolean isAdmin = SecurityUtils.getAuthenticationToken()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("cursos.analisar.solicitacoes") ||
                                a.getAuthority().equals("cursos.manter.cursos")))
                .orElse(false);

        Long usuarioId = getIdServidorLogado();

        if (!isAdmin && usuarioId == null) {
            return Page.empty(pageable);
        }

        Specification<SolicitacaoCurso> spec = solicitacaoSpecification.comFiltros(search, statusId, minStatusId,
                usuarioId, isAdmin);

        return solicitacaoRepository.findAll(spec, pageable).map(this::mapToResponseDto);
    }

    public java.util.List<SolicitacaoCursoResponseDto> listarTodas() {
        // Método legado, pode ser removido após transição completa do frontend
        return solicitacaoRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitacaoCursoResponseDto atualizar(Long id, SolicitacaoCursoRequestDto dto) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        validarPropriedadeOuAdmin(solicitacao);

        // A edição só é permitida se for RASCUNHO (1) ou DEVOLVIDO PARA AJUSTE (7)
        Long currentStatus = solicitacao.getStatus() != null ? solicitacao.getStatus().getId() : 1L;
        if (currentStatus != 1L && currentStatus != 7L) {
            throw new AppException(
                    "Solicitação no status atual não pode ser editada. O CEAF precisa Habilitar a Edição.");
        }

        // Limpar filhos para re-inserção (simplificação do mapeamento)
        solicitacao.getAgendas().clear();
        solicitacao.getMinistrantes().clear();
        solicitacaoRepository.saveAndFlush(solicitacao);

        // Re-mapear campos
        updateEntityFromDto(solicitacao, dto);

        // Se mudou para SUBMETIDO, apenas salva com o novo status
        if ("SUBMETIDO".equalsIgnoreCase(dto.getStatus())) {
            validarMinistrantesSeSubmetido(dto);
            Long novoStatusId = solicitacao.getNumeroPgaMpVirtual() == null ? 2L : 8L; // 2 = SUBMETIDO, 8 = RETIFICADO
            StatusTramitacao status = statusTramitacaoRepository.findById(novoStatusId)
                    .orElseThrow(() -> new AppException("Status " + novoStatusId + " não encontrado."));
            solicitacao.setStatus(status);
            return mapToResponseDto(solicitacaoRepository.save(solicitacao));
        }

        return mapToResponseDto(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public void excluir(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        validarPropriedadeOuAdmin(solicitacao);

        Long currentStatus = solicitacao.getStatus() != null ? solicitacao.getStatus().getId() : 1L;
        if (solicitacao.getNumeroPgaMpVirtual() != null || currentStatus != 1L) {
            throw new AppException(
                    "Não é possível excluir uma solicitação concluída ou que já foi enviada ao MPVirtual.");
        }

        solicitacaoRepository.delete(solicitacao);
    }

    @Transactional
    public SolicitacaoCursoResponseDto avaliarSolicitacao(Long id, Long statusId) {
        if (id == null) {
            throw new BusinessRuleException("O ID da solicitação é obrigatório.");
        }
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        if (statusId == null) {
            throw new BusinessRuleException("O ID do status é obrigatório.");
        }
        StatusTramitacao statusAlvo = statusTramitacaoRepository.findById(statusId)
                .orElseThrow(() -> new NotFoundException("Status não encontrado"));

        Long statusAtualId = solicitacao.getStatus() != null ? solicitacao.getStatus().getId() : 1L;

        // Regra: Bloquear qualquer alteração se já estiver CONVERTIDO (6)
        if (statusAtualId == 6L) {
            throw new BusinessRuleException(
                    "Esta solicitação já foi convertida em curso e não pode mais ser alterada.");
        }

        // Regra: Para Deferir/Homologar (3) ou Indeferir (4), a solicitação deve estar
        // SUBMETIDA (2) ou RETIFICADA (8) ou já avaliada (3 ou 4)
        if ((statusId == 3L || statusId == 4L) &&
                (statusAtualId != 2L && statusAtualId != 8L && statusAtualId != 3L && statusAtualId != 4L)) {
            throw new BusinessRuleException(
                    "A solicitação deve estar SUBMETIDA ou RETIFICADA para ser homologada ou indeferida.");
        }

        // Regra: Para Iniciar Protocolo/Análise (5), a solicitação deve estar SUBMETIDA
        // (2)
        if (statusId == 5L && statusAtualId != 2L) {
            throw new BusinessRuleException(
                    "Apenas solicitações SUBMETIDAS podem ser enviadas para análise.");
        }

        solicitacao.setStatus(statusAlvo);
        return mapToResponseDto(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public SolicitacaoCursoResponseDto habilitarEdicao(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        SecurityUtils.getAuthenticationToken().ifPresent(auth -> {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("cursos.analisar.solicitacoes") ||
                            a.getAuthority().equals("cursos.manter.cursos"));

            if (!isAdmin) {
                throw new AppException("Acesso negado: apenas o administrador pode habilitar a edição.");
            }
        });

        Long statusAtualId = solicitacao.getStatus() != null ? solicitacao.getStatus().getId() : 1L;
        if (statusAtualId == 6L) {
            throw new BusinessRuleException("Não é possível habilitar edição para uma solicitação já convertida.");
        }

        StatusTramitacao statusAlvo = statusTramitacaoRepository.findById(7L)
                .orElseThrow(() -> new NotFoundException("Status de devolução para ajuste não encontrado"));

        solicitacao.setStatus(statusAlvo);
        return mapToResponseDto(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public SolicitacaoCursoResponseDto protocolarSolicitacao(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        Long statusId = solicitacao.getStatus() != null ? solicitacao.getStatus().getId() : 1L;

        // Regra: Apenas solicitações SUBMETIDAS (2), EM ANÁLISE (5) ou RETIFICADAS (8)
        // podem ir ao MPVirtual
        if (statusId != 2L && statusId != 5L && statusId != 8L) {
            throw new BusinessRuleException(
                    "Apenas solicitações submetidas, em análise ou retificadas podem ser protocoladas.");
        }

        // Fluxo de RETIFICAÇÃO (Juntada de Peça e Movimentação)
        if (statusId == 8L || solicitacao.getNumeroPgaMpVirtual() != null) {
            if (solicitacao.getNumeroPgaMpVirtual() == null) {
                throw new BusinessRuleException("Solicitação retificada sem número de processo no MPVirtual.");
            }
            return executarProtocoloRetificacao(solicitacao);
        }

        // Fluxo Original de ABERTURA de Processo
        if (solicitacao.getNumeroPgaMpVirtual() != null) {
            throw new BusinessRuleException("Esta solicitação já possui um número de processo no MPVirtual.");
        }

        MPVirtualProcessoResponseDto integracaoResponse = gerarEEnviarParaMpVirtual(solicitacao);

        SolicitacaoCursoResponseDto responseDto = mapToResponseDto(solicitacao);
        responseDto.setMpVirtualPayload(integracaoResponse);

        return responseDto;
    }

    private SolicitacaoCursoResponseDto executarProtocoloRetificacao(SolicitacaoCurso solicitacao) {
        log.info("Iniciando fluxo de retificação para solicitação {} no processo {}", solicitacao.getId(),
                solicitacao.getNumeroPgaMpVirtual());

        try {
            // 1. Identificar Requerente
            Integer idPessoaRequestor = null;
            Long idServidor = solicitacao.getIdServidorInteressado();
            if (idServidor != null) {
                idPessoaRequestor = servidorRepository.buscarServidorPorId(idServidor.intValue())
                        .map(mppb.restapi.cursos.feature.common.projection.ServidorProjection::getIdPessoa)
                        .orElse(null);
            }
            if (idPessoaRequestor == null)
                idPessoaRequestor = 237;

            Long idOrgaoCeaf = 16L;

            // 2. Passo 1: Criar Minuta (se ainda não existir uma minuta pendente de
            // movimento)
            if (solicitacao.getIdMinuta() == null) {
                byte[] pdfGerado = reportService.gerarPdfSolicitacao(solicitacao);

                // IMPORTANTE: Para criar minuta, deve-se usar o ID numérico do processo, não o
                // número formatado
                String idProcessoStr = (solicitacao.getIdPgaMpVirtual() != null)
                        ? solicitacao.getIdPgaMpVirtual().toString()
                        : solicitacao.getNumeroPgaMpVirtual();

                mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualCriarMinutaResponseDto minutaResponse = mpVirtualService
                        .criarMinuta(
                                idProcessoStr,
                                "Retificação de Solicitação: " + solicitacao.getNomeCurso(),
                                idOrgaoCeaf,
                                idPessoaRequestor,
                                pdfGerado);

                if (minutaResponse != null && minutaResponse.getIdMinuta() != null) {
                    solicitacao.setIdMinuta(minutaResponse.getIdMinuta());
                    solicitacaoRepository.saveAndFlush(solicitacao);
                } else {
                    throw new AppException("A API do MPVirtual não retornou um ID de minuta válido.");
                }
            } else {
                log.info("Aproveitando minuta já existente (ID: {}) para a retificação da solicitação {}",
                        solicitacao.getIdMinuta(), solicitacao.getId());
            }

            // 3. Passo 2: Movimentar Minuta
            Long idMovimentoRetificacao = 400920057L;
            mpVirtualService.movimentarMinuta(solicitacao.getIdPgaMpVirtual(), solicitacao.getIdMinuta(),
                    idMovimentoRetificacao, idOrgaoCeaf);

            // 4. Finalização: Limpa ID Minuta e volta Status para PROTOCOLADO (3)
            solicitacao.setIdMinuta(null);
            statusTramitacaoRepository.findById(3L).ifPresent(solicitacao::setStatus);
            solicitacaoRepository.save(solicitacao);

            log.info("Retificação da solicitação {} finalizada com sucesso.", solicitacao.getId());

            return mapToResponseDto(solicitacao);

        } catch (Exception e) {
            log.error("Erro no fluxo de retificação MPVirtual.", e);
            throw new AppException(e, "Erro ao processar retificação no MPVirtual: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public MPVirtualMetadataDto obterMetadataProtocolo(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        Integer idPessoaRequestor = null;
        Long idServidor = solicitacao.getIdServidorInteressado();

        if (idServidor != null) {
            idPessoaRequestor = servidorRepository.buscarServidorPorId(idServidor.intValue())
                    .map(mppb.restapi.cursos.feature.common.projection.ServidorProjection::getIdPessoa)
                    .orElse(null);
        }

        if (idPessoaRequestor == null) {
            idPessoaRequestor = 237;
        }

        return mpVirtualService.construirMetadata(extrairResumo(solicitacao), idPessoaRequestor);
    }

    private String extrairResumo(SolicitacaoCurso solicitacao) {
        return StringUtils.hasText(solicitacao.getObservacoesGerais())
                ? solicitacao.getObservacoesGerais()
                : "Solicitação gerada via sistema Cursos-Web";
    }

    @Transactional
    public Long converterParaCurso(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        // Apenas solicitações PROTOCOLADAS (ID 3) podem ser convertidas
        if (solicitacao.getStatus() == null || !Long.valueOf(3L).equals(solicitacao.getStatus().getId())) {
            throw new BusinessRuleException(
                    "Apenas solicitações PROTOCOLADAS podem ser convertidas em curso. Status atual: "
                            + (solicitacao.getStatus() != null
                                    ? solicitacao.getStatus().getId() + " - " + solicitacao.getStatus().getDescricao()
                                    : "null"));
        }

        Curso novoCurso = new Curso();
        novoCurso.setTitulo(solicitacao.getNomeCurso());
        novoCurso.setModalidade(solicitacao.getModalidade());
        novoCurso.setTipoEvento(solicitacao.getTipoEvento());
        novoCurso.setLinkProgramacao(null); // Poderia vir do GED depois
        novoCurso.setDescricao("Curso gerado a partir da solicitação #" + solicitacao.getId() + " ("
                + solicitacao.getNumeroPgaMpVirtual() + ")");

        // Mapeamento das datas e cálculo da carga horária real
        if (!solicitacao.getAgendas().isEmpty()) {
            Duration totalDuration = Duration.ZERO;
            LocalDateTime dataInicial = null;
            LocalDateTime dataFinal = null;

            for (SolicitacaoAgenda agenda : solicitacao.getAgendas()) {
                // Cálculo da duração diária
                Duration dailyDuration = Duration.between(agenda.getHorarioInicio(), agenda.getHorarioFim());

                // Número de dias (inclusive)
                long days = ChronoUnit.DAYS.between(agenda.getDataInicio(), agenda.getDataFim()) + 1;

                // Carga horária total deste item da agenda
                totalDuration = totalDuration.plus(dailyDuration.multipliedBy(days));

                // Determinação do período total do curso para as datas de realização
                LocalDateTime inicio = agenda.getDataInicio().atTime(agenda.getHorarioInicio());
                LocalDateTime fim = agenda.getDataFim().atTime(agenda.getHorarioFim());

                // Determinação do período total do curso
                if (dataInicial == null || inicio.isBefore(dataInicial)) {
                    dataInicial = inicio;
                }
                if (dataFinal == null || fim.isAfter(dataFinal)) {
                    dataFinal = fim;
                }
            }

            novoCurso.setRealizacaoDataInicial(dataInicial);
            novoCurso.setRealizacaoDataFinal(dataFinal);

            log.info("Calculando carga horária para Solicitação {}. Total Agendas: {}. Duração acumulada: {}", id,
                    solicitacao.getAgendas().size(), totalDuration);

            long hours = totalDuration.toHours();
            int minutes = totalDuration.toMinutesPart();
            int seconds = totalDuration.toSecondsPart();
            String chCalculada = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            log.info("Convertendo Solicitação {} para Curso. Carga Horária Calculada: {}", id, chCalculada);
            novoCurso.setCargaHoraria(chCalculada);
        } else {
            // Fallback se não houver agendas
            log.warn(
                    "Solicitação {} convertida sem agendas. Carga Horária definida como 00:00:00. Verifique se a solicitação possui itens na lista de agendas.",
                    id);
            novoCurso.setCargaHoraria("00:00:00");
        }

        // Definir Entidade Tutora padrão: Use a da solicitação ou fallback p/ CEAF
        if (solicitacao.getEntidadeTutora() != null) {
            novoCurso.setEntidadeTutora(solicitacao.getEntidadeTutora());
        } else {
            // Fallback para CEAF (ID 1 conforme verificado no banco)
            entidadeTutoraRepository.findById(1L).ifPresentOrElse(
                    novoCurso::setEntidadeTutora,
                    () -> {
                        // Fallback secundário por nome caso o ID mude
                        entidadeTutoraRepository.findAll().stream()
                                .filter(et -> et.getNome().toUpperCase().contains("CEAF"))
                                .findFirst()
                                .ifPresent(novoCurso::setEntidadeTutora);
                    });
        }

        // Copiar Públicos-Alvo
        novoCurso.setPublicosAlvo(new HashSet<>(solicitacao.getPublicosAlvo()));

        Curso cursoSalvo = cursoService.save(novoCurso);

        // Copiar ministrantes
        solicitacao.getMinistrantes().forEach(sm -> {
            AssociarMinistranteDto tDto = new AssociarMinistranteDto();
            tDto.setIdServidor(sm.getIdServidor() != null ? sm.getIdServidor().intValue() : null);
            tDto.setNome(sm.getNomeMinistrante());
            tDto.setEmail(sm.getEmail());
            tDto.setTelefone(sm.getTelefone());
            tDto.setPapelId(sm.getPapelMinistrante() != null ? sm.getPapelMinistrante().getId() : 1L); // Default
                                                                                                       // Palestrante

            if (sm.getIdServidor() != null) {
                tDto.setTipo("INTERNO");
            } else {
                tDto.setTipo("EXTERNO");
                String cleanCpf = sm.getCpf() != null ? sm.getCpf().replaceAll("\\D", "") : null;
                tDto.setCpf(cleanCpf);
                if (sm.getMinistrante() != null) {
                    tDto.setMinistranteId(sm.getMinistrante().getId());
                }
            }

            tDto.setIdFoto(sm.getIdFoto());
            tDto.setIdCurriculo(sm.getIdCurriculo());

            cursoService.addMinistranteToCurso(cursoSalvo.getId(), tDto);
        });

        // Marcar como CONVERIDO (Novo ID 6)
        statusTramitacaoRepository.findById(6L).ifPresent(solicitacao::setStatus);
        solicitacao.setIdCursoGerado(cursoSalvo.getId());
        solicitacaoRepository.save(solicitacao);
        return cursoSalvo.getId();
    }

    private void validarPropriedadeOuAdmin(SolicitacaoCurso solicitacao) {
        SecurityUtils.getAuthenticationToken().ifPresent(auth -> {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("cursos.analisar.solicitacoes") ||
                            a.getAuthority().equals("cursos.manter.cursos"));

            Long idCorreto = getIdServidorLogado();
            if (!isAdmin && (idCorreto == null || !idCorreto.equals(solicitacao.getIdServidorInteressado()))) {
                throw new AppException("Acesso negado: você não é o proprietário desta solicitação.");
            }
        });
    }

    private Long getIdServidorLogado() {
        return SecurityUtils.getAuthenticationToken()
                .map(auth -> {
                    String login = auth.getUsuario().getLogin();
                    Long usuarioId = auth.getUsuario().getId();

                    if (login != null) {
                        Optional<Long> idServidor = servidorRepository.findIdServidorByLogin(login)
                                .map(Integer::longValue);

                        if (idServidor.isPresent()) {
                            return idServidor.get();
                        } else {
                            return usuarioId;
                        }
                    }
                    return usuarioId;
                })
                .orElseGet(() -> {
                    return null;
                });
    }

    public byte[] gerarPdf(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));
        return reportService.gerarPdfSolicitacao(solicitacao);
    }

    private void updateEntityFromDto(SolicitacaoCurso solicitacao, SolicitacaoCursoRequestDto dto) {
        solicitacao.setNomeCurso(dto.getNomeCurso());
        solicitacao.setEstimativaParticipantes(dto.getEstimativaParticipantes());

        // Lookups
        solicitacao.setModalidade(modalidadeRepository.findById(dto.getIdModalidade())
                .orElseThrow(() -> new NotFoundException("Modalidade não encontrada")));
        solicitacao.setTipoEvento(tipoEventoRepository.findById(dto.getIdTipoEvento())
                .orElseThrow(() -> new NotFoundException("Tipo de Evento não encontrado")));
        solicitacao.setPublicosAlvo(publicoAlvoRepository.findAllById(dto.getIdsPublicoAlvo()));

        if (dto.getIdLocalEvento() != null) {
            solicitacao.setLocalEvento(localEventoRepository.findById(dto.getIdLocalEvento())
                    .orElseThrow(() -> new NotFoundException("Local do evento não encontrado")));
        } else {
            solicitacao.setLocalEvento(null);
        }

        if (dto.getIdEntidadeTutora() != null) {
            solicitacao.setEntidadeTutora(entidadeTutoraRepository.findById(dto.getIdEntidadeTutora())
                    .orElseThrow(() -> new NotFoundException("Entidade Tutora não encontrada")));
        } else {
            solicitacao.setEntidadeTutora(null);
        }

        solicitacao.setDescricaoLocalExterno(dto.getDescricaoLocalExterno());
        solicitacao.setTemOrgaoExterno(dto.getTemOrgaoExterno());
        solicitacao.setNomeOrgaoExterno(dto.getNomeOrgaoExterno());

        // Logística e Financeiro
        solicitacao.setCardDivulgacao(dto.getCardDivulgacao());
        solicitacao.setComunicacaoImprensa(dto.getComunicacaoImprensa());
        solicitacao.setOficioConvite(dto.getOficioConvite());
        solicitacao.setCoffeeBreak(dto.getCoffeeBreak());
        solicitacao.setKitParticipante(dto.getKitParticipante());
        solicitacao.setProgramacaoImpressa(dto.getProgramacaoImpressa());
        solicitacao.setTrasladoMinistrante(dto.getTrasladoMinistrante());
        solicitacao.setPagamentoHonorario(dto.getPagamentoHonorario());
        solicitacao.setPassagemAerea(dto.getPassagemAerea());
        solicitacao.setHospedagem(dto.getHospedagem());
        solicitacao.setObservacoesGerais(dto.getObservacoesGerais());
        solicitacao.setIdArquivoProgramacaoGed(dto.getIdArquivoProgramacaoGed());

        // Filhos: Agendas
        if (dto.getAgendas() != null) {
            dto.getAgendas().forEach(agendaDto -> {
                SolicitacaoAgenda agenda = new SolicitacaoAgenda();
                agenda.setDataInicio(agendaDto.getDataInicio());
                agenda.setDataFim(agendaDto.getDataFim());
                agenda.setHorarioInicio(agendaDto.getHorarioInicio());
                agenda.setHorarioFim(agendaDto.getHorarioFim());
                solicitacao.addAgenda(agenda);
            });
        }

        // Filhos: Ministrantes
        if (dto.getMinistrantes() != null) {
            dto.getMinistrantes().forEach(minDto -> {
                SolicitacaoMinistrante ministrante = new SolicitacaoMinistrante();
                ministrante.setIdServidor(minDto.getIdServidor());
                ministrante.setNomeMinistrante(minDto.getNomeMinistrante());
                ministrante.setTelefone(minDto.getTelefone());
                String cleanCpf = minDto.getCpf() != null ? minDto.getCpf().replaceAll("\\D", "") : null;
                ministrante.setCpf(cleanCpf);
                ministrante.setEmail(minDto.getEmail());
                ministrante.setIdCurriculo(minDto.getIdCurriculo());
                ministrante.setIdFoto(minDto.getIdFoto());

                if (minDto.getMinistranteId() != null) {
                    ministranteRepositoryRef.findById(minDto.getMinistranteId())
                            .ifPresent(ministrante::setMinistrante);
                }

                PapelMinistrante papel = papelMinistranteRepository.findById(minDto.getIdPapelMinistrante())
                        .orElseThrow(() -> new NotFoundException("Papel de ministrante não encontrado"));
                ministrante.setPapelMinistrante(papel);

                solicitacao.addMinistrante(ministrante);
            });
        }
    }

    private void validarMinistrantesSeSubmetido(mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoRequestDto dto) {
        if ("SUBMETIDO".equalsIgnoreCase(dto.getStatus())) {
            if (dto.getMinistrantes() == null || dto.getMinistrantes().isEmpty()) {
                throw new mppb.restapi.common.infrastructure.exception.BusinessRuleException(
                        "É necessário cadastrar pelo menos um ministrante para enviar a solicitação.");
            }
        }
    }
}

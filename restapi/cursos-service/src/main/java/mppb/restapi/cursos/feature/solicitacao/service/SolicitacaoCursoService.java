package mppb.restapi.cursos.feature.solicitacao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualMetadataDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoResponseDto;
import mppb.restapi.common.infrastructure.service.mpvirtual.MPVirtualIntegrationService;
import mppb.restapi.cursos.feature.lookup.entities.LocalEvento;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;
import mppb.restapi.cursos.feature.lookup.repository.LocalEventoRepository;
import mppb.restapi.cursos.feature.lookup.repository.ModalidadeRepository;
import mppb.restapi.cursos.feature.lookup.repository.PublicoAlvoRepository;
import mppb.restapi.cursos.feature.lookup.repository.TipoEventoRepository;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.cursos.feature.lookup.entities.StatusTramitacao;
import mppb.restapi.cursos.feature.lookup.repository.PapelMinistranteRepository;
import mppb.restapi.cursos.feature.lookup.repository.StatusTramitacaoRepository;
import mppb.restapi.cursos.feature.cadastro.repository.EntidadeTutoraRepository;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoRequestDto;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoResponseDto;
import org.springframework.util.StringUtils;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoAgenda;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoCurso;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoMinistrante;
import mppb.restapi.cursos.feature.solicitacao.repository.SolicitacaoCursoRepository;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.dto.AssociarMinistranteDto;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import mppb.restapi.cursos.feature.common.repository.ServidorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitacaoCursoService {

    private final SolicitacaoCursoRepository solicitacaoRepository;
    private final SolicitacaoReportService reportService;
    private final MPVirtualIntegrationService mpVirtualService;

    // Lookups
    private final ModalidadeRepository modalidadeRepository;
    private final PublicoAlvoRepository publicoAlvoRepository;
    private final LocalEventoRepository localEventoRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final StatusTramitacaoRepository statusTramitacaoRepository;
    private final PapelMinistranteRepository papelMinistranteRepository;
    private final mppb.restapi.cursos.feature.ministrante.repository.MinistranteRepository ministranteRepository;
    private final CursoService cursoService;
    private final ServidorRepository servidorRepository;
    private final EntidadeTutoraRepository entidadeTutoraRepository;

    /**
     * Fluxo de criação de solicitação:
     * 1. Salva entidade com status INICIAL ou SUBMETIDO.
     * 2. O protocolo no MPVirtual NÃO ocorre aqui, deve ser disparado manualmente pelo CEAF.
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
        }
        solicitacao.setIdOrgaoInteressado(1L); // Mock ou extrair do token se disponível

        // Se for RASCUNHO, usamos status 1. Se SUBMETIDO, usamos 2 (Em Análise)
        Long idStatus = "SUBMETIDO".equalsIgnoreCase(dto.getStatus()) ? 2L : 1L;

        StatusTramitacao status = statusTramitacaoRepository.findById(idStatus)
                .orElseThrow(() -> new AppException("Status de tramitação " + idStatus + " não encontrado."));
        solicitacao.setStatus(status);

        // Salva a entidade primeiro para ter o ID
        SolicitacaoCurso solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        return mapToResponseDto(solicitacaoSalva);
    }

    private MPVirtualMetadataDto gerarEEnviarParaMpVirtual(SolicitacaoCurso solicitacaoSalva) {
        // 2. Gerar PDF do PGA
        byte[] pdfGerado = reportService.gerarPdfSolicitacao(solicitacaoSalva);

        // 3. Enviar para o MPVirtual
        try {
            // O requerente e o interessado no MPVirtual deve ser o usuário que solicitou o curso inicialmente
            Integer idPessoaRequestor = null;
            Long idServidor = solicitacaoSalva.getIdServidorInteressado();
            
            if (idServidor != null) {
                idPessoaRequestor = servidorRepository.buscarServidorPorId(idServidor.intValue())
                        .map(mppb.restapi.cursos.feature.common.projection.ServidorProjection::getIdPessoa)
                        .orElse(null);
            }

            // Fallback para usuário mock se não for possível identificar o servidor (apenas para ambiente de desenvolvimento)
            if (idPessoaRequestor == null) {
                idPessoaRequestor = 237; 
                log.warn("Pessoa interessada não identificada para o servidor {}. Usando fallback 237.", idServidor);
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

            // 4. Recebido o número do processo, atualiza e muda o status para PROTOCOLADO (7)
            if (integracaoResponse != null && integracaoResponse.getNumeroProcesso() != null) {
                solicitacaoSalva.setNumeroPgaMpVirtual(integracaoResponse.getNumeroProcesso());
                statusTramitacaoRepository.findById(7L).ifPresent(solicitacaoSalva::setStatus);
                
                solicitacaoRepository.save(solicitacaoSalva);
                log.info("Solicitação {} protocolada no MPVirtual com número {}", solicitacaoSalva.getId(), integracaoResponse.getNumeroProcesso());
            } else {
                throw new AppException("A API do MPVirtual não retornou um número de processo válido.");
            }

            return result.metadata();
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
        Modalidade modalidade = modalidadeRepository.findById(dto.getIdModalidade())
                .orElseThrow(() -> new NotFoundException("Modalidade não encontrada"));
        solicitacao.setModalidade(modalidade);

        TipoEvento tipoEvento = tipoEventoRepository.findById(dto.getIdTipoEvento())
                .orElseThrow(() -> new NotFoundException("Tipo de Evento não encontrado"));
        solicitacao.setTipoEvento(tipoEvento);

        List<PublicoAlvo> publicosAlvo = publicoAlvoRepository.findAllById(dto.getIdsPublicoAlvo());
        if (publicosAlvo.isEmpty() && dto.getIdsPublicoAlvo() != null && !dto.getIdsPublicoAlvo().isEmpty()) {
            throw new NotFoundException("Nenhum público-alvo encontrado para os IDs fornecidos");
        }
        solicitacao.setPublicosAlvo(publicosAlvo);

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
        solicitacao.setIdentificacaoOrgaoMembros(dto.getIdentificacaoOrgaoMembros());
        solicitacao.setEspecificacaoOrgaoMembros(dto.getEspecificacaoOrgaoMembros());

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
                    ministranteRepository.findById(minDto.getMinistranteId())
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

        if (entity.getIdServidorInteressado() != null) {
            servidorRepository.buscarServidorPorId(entity.getIdServidorInteressado().intValue())
                    .ifPresentOrElse(
                            servidor -> dto.setNomeSolicitante(servidor.getNome()),
                            () -> {
                                if (Long.valueOf(998L).equals(entity.getIdServidorInteressado())) {
                                    dto.setNomeSolicitante("Solicitante de Teste");
                                } else if (Long.valueOf(999L).equals(entity.getIdServidorInteressado())) {
                                    dto.setNomeSolicitante("Administrador de Teste");
                                }
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
            dto.setPublicosAlvoDescricao(entity.getPublicosAlvo().stream().map(PublicoAlvo::getNome).toList());
        }
        if (entity.getLocalEvento() != null) {
            dto.setIdLocalEvento(entity.getLocalEvento().getId());
        }

        if (entity.getEntidadeTutora() != null) {
            dto.setIdEntidadeTutora(entity.getEntidadeTutora().getId());
            dto.setEntidadeTutoraNome(entity.getEntidadeTutora().getNome());
        }

        // Campos de texto e booleanos
        dto.setDescricaoLocalExterno(entity.getDescricaoLocalExterno());
        dto.setIdentificacaoOrgaoMembros(entity.getIdentificacaoOrgaoMembros());
        dto.setEspecificacaoOrgaoMembros(entity.getEspecificacaoOrgaoMembros());
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
        dto.setDataCriacao(entity.getDataCriacao());
        dto.setDataModificacao(entity.getDataModificacao());

        return dto;
    }

    public SolicitacaoCursoResponseDto buscarPorId(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        validarPropriedadeOuAdmin(solicitacao);

        return mapToResponseDto(solicitacao);
    }

    public java.util.List<SolicitacaoCursoResponseDto> listarTodas() {
        boolean isAdmin = SecurityUtils.getAuthenticationToken()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("cursos.analisar.solicitacoes") ||
                                a.getAuthority().equals("cursos.manter.cursos")))
                .orElse(false);

        if (isAdmin) {
            // Admin vê todas as SUBMETIDAS ou em análise (não vê RASCUNHOS)
            return solicitacaoRepository.findAll().stream()
                    .filter(s -> {
                        if (s.getStatus() == null)
                            return false;
                        String desc = s.getStatus().getDescricao();
                        return s.getStatus().getId() >= 2 &&
                                (desc == null || !desc.equalsIgnoreCase("RASCUNHO"));
                    })
                    .map(this::mapToResponseDto)
                    .toList();
        } else {
            // Usuário normal vê apenas as suas
            Long usuarioId = getIdServidorLogado();

            if (usuarioId == null)
                return Collections.emptyList();

            return solicitacaoRepository.findAll().stream()
                    .filter(s -> usuarioId.equals(s.getIdServidorInteressado()))
                    .map(this::mapToResponseDto)
                    .toList();
        }
    }

    @Transactional
    public SolicitacaoCursoResponseDto atualizar(Long id, SolicitacaoCursoRequestDto dto) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        validarPropriedadeOuAdmin(solicitacao);

        // Se já foi submetida, não permitir edição (regrade negócio)
        if (solicitacao.getNumeroPgaMpVirtual() != null) {
            throw new AppException("Solicitação já submetida ao MPVirtual não pode ser editada.");
        }

        // Limpar filhos para re-inserção (simplificação do mapeamento)
        solicitacao.getAgendas().clear();
        solicitacao.getMinistrantes().clear();
        solicitacaoRepository.saveAndFlush(solicitacao);

        // Re-mapear campos
        updateEntityFromDto(solicitacao, dto);

        // Se mudou para SUBMETIDO, apenas salva com o novo status
        if ("SUBMETIDO".equalsIgnoreCase(dto.getStatus())) {
            StatusTramitacao status = statusTramitacaoRepository.findById(2L)
                    .orElseThrow(() -> new AppException("Status SUBMETIDO não encontrado."));
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

        if (solicitacao.getNumeroPgaMpVirtual() != null) {
            throw new AppException("Não é possível excluir uma solicitação já enviada ao MPVirtual.");
        }

        solicitacaoRepository.delete(solicitacao);
    }

    @Transactional
    public SolicitacaoCursoResponseDto avaliarSolicitacao(Long id, Long statusId) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        StatusTramitacao statusAlvo = statusTramitacaoRepository.findById(statusId)
                .orElseThrow(() -> new NotFoundException("Status não encontrado"));

        Long statusAtualId = solicitacao.getStatus() != null ? solicitacao.getStatus().getId() : 1L;

        // Regra: Bloquear qualquer alteração se já estiver CONVERTIDO (6)
        if (statusAtualId == 6L) {
            throw new BusinessRuleException(
                    "Esta solicitação já foi convertida em curso e não pode mais ser alterada.");
        }

        // Regra: Para Deferir (3) ou Indeferir (4), a solicitação deve estar EM ANÁLISE (5),
        // PROTOCOLADA (7) ou já avaliada (3 ou 4)
        if ((statusId == 3L || statusId == 4L) && 
            (statusAtualId != 5L && statusAtualId != 7L && statusAtualId != 3L && statusAtualId != 4L)) {
            throw new BusinessRuleException("A solicitação deve estar em status compatível (Análise ou Protocolada) para ser deferida ou indeferida.");
        }

        // Regra: Para Iniciar Protocolo (ação manual do CEAF), a solicitação deve estar SUBMETIDA (2)
        if (statusId == 5L && statusAtualId != 2L) {
            throw new BusinessRuleException("Apenas solicitações SUBMETIDAS podem ser enviadas para análise/protocolo.");
        }

        solicitacao.setStatus(statusAlvo);
        return mapToResponseDto(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public SolicitacaoCursoResponseDto protocolarSolicitacao(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        Long statusId = solicitacao.getStatus() != null ? solicitacao.getStatus().getId() : 1L;

        // Regra: Apenas solicitações SUBMETIDAS (2) ou EM ANÁLISE (5) podem ser protocoladas
        if (statusId != 2L && statusId != 5L) {
            throw new BusinessRuleException("Apenas solicitações submetidas ou em análise podem ser protocoladas.");
        }

        if (solicitacao.getNumeroPgaMpVirtual() != null) {
            throw new BusinessRuleException("Esta solicitação já possui um número de processo no MPVirtual.");
        }

        MPVirtualMetadataDto metadata = gerarEEnviarParaMpVirtual(solicitacao);
        
        SolicitacaoCursoResponseDto responseDto = mapToResponseDto(solicitacao);
        responseDto.setMpVirtualPayload(metadata);

        return responseDto;
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
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        // Apenas solicitações DEFERIDAS (ID 3) podem ser convertidas
        if (solicitacao.getStatus() == null || !Long.valueOf(3L).equals(solicitacao.getStatus().getId())) {
            throw new BusinessRuleException(
                    "Apenas solicitações DEFERIDAS podem ser convertidas em curso. Status atual: "
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

            // Formatação da carga horária total (HH:MM:SS)
            long hours = totalDuration.toHours();
            int minutes = totalDuration.toMinutesPart();
            int seconds = totalDuration.toSecondsPart();
            String chCalculada = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            log.info("Convertendo Solicitação {} para Curso. Carga Horária Calculada: {}. Agendas processadas: {}", id,
                    chCalculada, solicitacao.getAgendas().size());
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
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
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
        solicitacao.setIdentificacaoOrgaoMembros(dto.getIdentificacaoOrgaoMembros());
        solicitacao.setEspecificacaoOrgaoMembros(dto.getEspecificacaoOrgaoMembros());

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
                    ministranteRepository.findById(minDto.getMinistranteId())
                            .ifPresent(ministrante::setMinistrante);
                }

                PapelMinistrante papel = papelMinistranteRepository.findById(minDto.getIdPapelMinistrante())
                        .orElseThrow(() -> new NotFoundException("Papel de ministrante não encontrado"));
                ministrante.setPapelMinistrante(papel);

                solicitacao.addMinistrante(ministrante);
            });
        }
    }
}

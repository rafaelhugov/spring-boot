package mppb.restapi.cursos.feature.solicitacao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoResponseDto;
import mppb.restapi.common.infrastructure.service.mpvirtual.MPVirtualIntegrationService;
import mppb.restapi.cursos.feature.lookup.entities.LocalEvento;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.cursos.feature.lookup.repository.LocalEventoRepository;
import mppb.restapi.cursos.feature.lookup.repository.ModalidadeRepository;
import mppb.restapi.cursos.feature.lookup.repository.PublicoAlvoRepository;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.cursos.feature.lookup.entities.StatusTramitacao;
import mppb.restapi.cursos.feature.lookup.repository.PapelMinistranteRepository;
import mppb.restapi.cursos.feature.lookup.repository.StatusTramitacaoRepository;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoRequestDto;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoResponseDto;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoAgenda;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoCurso;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoMinistrante;
import mppb.restapi.cursos.feature.solicitacao.repository.SolicitacaoCursoRepository;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.dto.AssociarMinistranteDto;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

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
    private final StatusTramitacaoRepository statusTramitacaoRepository;
    private final PapelMinistranteRepository papelMinistranteRepository;
    private final mppb.restapi.cursos.feature.ministrante.repository.MinistranteRepository ministranteRepository;
    private final CursoService cursoService;

    /**
     * Fluxo completo de criação de solicitação:
     * 1. Salva entidade
     * 2. Gera PDF
     * 3. Envia p/ MPVirtual
     * 4. Atualiza a entidade com o número do processo
     * 
     * @param dto Os dados da solicitação preenchidos pelo usuário
     */
    @Transactional
    public SolicitacaoCursoResponseDto solicitarCurso(SolicitacaoCursoRequestDto dto) {
        log.info("Processando nova solicitação de curso: {}", dto.getNomeCurso());

        // 1. Mapear DTO para Entidade
        SolicitacaoCurso solicitacao = mapToEntity(dto);

        // Identificação do Interessado via Token de Autênticação
        SecurityUtils.getAuthenticationToken().ifPresent(auth -> {
            solicitacao.setIdServidorInteressado(auth.getUsuario().getId());
            solicitacao.setIdOrgaoInteressado(1L); // Mock ou extrair do token se disponível
        });

        // Se for RASCUNHO, usamos status 1. Se SUBMETIDO, usamos 2 (Em Análise)
        Long idStatus = "SUBMETIDO".equalsIgnoreCase(dto.getStatus()) ? 2L : 1L;

        StatusTramitacao status = statusTramitacaoRepository.findById(idStatus)
                .orElseThrow(() -> new AppException("Status de tramitação " + idStatus + " não encontrado."));
        solicitacao.setStatus(status);

        // Salva a entidade primeiro para ter o ID
        SolicitacaoCurso solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        // 2. Se for SUBMETIDO, faz o fluxo completo (PDF + MPVirtual)
        if ("SUBMETIDO".equalsIgnoreCase(dto.getStatus())) {
            gerarEEnviarParaMpVirtual(solicitacaoSalva, dto);
        }

        return mapToResponseDto(solicitacaoSalva);
    }

    private void gerarEEnviarParaMpVirtual(SolicitacaoCurso solicitacaoSalva, SolicitacaoCursoRequestDto dto) {
        // 2. Gerar PDF do PGA
        byte[] pdfGerado = reportService.gerarPdfSolicitacao(solicitacaoSalva);

        // 3. Enviar para o MPVirtual
        try {
            Long usuarioLogadoMock = dto.getMinistrantes() != null && !dto.getMinistrantes().isEmpty()
                    && dto.getMinistrantes().get(0).getIdServidor() != null
                            ? dto.getMinistrantes().get(0).getIdServidor().longValue()
                            : 12345L;

            MPVirtualProcessoResponseDto integracaoResponse = mpVirtualService.abrirProcessoCurso(
                    "Solicitação de Curso: " + solicitacaoSalva.getNomeCurso(),
                    dto.getObservacoesGerais() != null ? dto.getObservacoesGerais()
                            : "Solicitação gerada via sistema Cursos-Web",
                    usuarioLogadoMock,
                    pdfGerado);

            // 4. Atualizar o registro com os dados do MPVirtual
            solicitacaoSalva.setNumeroPgaMpVirtual(integracaoResponse.getNumeroProcesso());
            solicitacaoRepository.save(solicitacaoSalva);
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

        if (entity.getStatus() != null) {
            dto.setStatusDescricao(entity.getStatus().getDescricao());
        }
        if (entity.getModalidade() != null) {
            dto.setIdModalidade(entity.getModalidade().getId());
            dto.setModalidadeDescricao(entity.getModalidade().getDescricao());
        }
        if (entity.getPublicosAlvo() != null && !entity.getPublicosAlvo().isEmpty()) {
            dto.setIdsPublicoAlvo(entity.getPublicosAlvo().stream().map(PublicoAlvo::getId).toList());
            dto.setPublicosAlvoDescricao(entity.getPublicosAlvo().stream().map(PublicoAlvo::getNome).toList());
        }
        if (entity.getLocalEvento() != null) {
            dto.setIdLocalEvento(entity.getLocalEvento().getId());
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
                md.setEmail(m.getEmail());
                md.setIdFoto(m.getIdFoto());
                md.setIdCurriculo(m.getIdCurriculo());
                return md;
            }).toList());
        }

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
            Long usuarioId = SecurityUtils.getAuthenticationToken()
                    .map(auth -> auth.getUsuario().getId())
                    .orElse(null);

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

        // Se mudou para SUBMETIDO, processa
        if ("SUBMETIDO".equalsIgnoreCase(dto.getStatus())) {
            StatusTramitacao status = statusTramitacaoRepository.findById(2L)
                    .orElseThrow(() -> new AppException("Status SUBMETIDO não encontrado."));
            solicitacao.setStatus(status);
            SolicitacaoCurso salva = solicitacaoRepository.save(solicitacao);
            gerarEEnviarParaMpVirtual(salva, dto);
            return mapToResponseDto(salva);
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

        StatusTramitacao status = statusTramitacaoRepository.findById(statusId)
                .orElseThrow(() -> new NotFoundException("Status não encontrado"));

        solicitacao.setStatus(status);
        return mapToResponseDto(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public void converterParaCurso(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        if (solicitacao.getStatus() == null || solicitacao.getStatus().getId() != 3L) { // 3 = Deferido (assumido)
            // throw new AppException("Apenas solicitações DEFERIDAS podem ser convertidas
            // em curso.");
        }

        Curso novoCurso = new Curso();
        novoCurso.setTitulo(solicitacao.getNomeCurso());
        novoCurso.setModalidade(solicitacao.getModalidade());
        // ... mapear outros campos (simplificado aqui para o fluxo)

        Curso cursoSalvo = cursoService.save(novoCurso);

        // Copiar ministrantes
        solicitacao.getMinistrantes().forEach(sm -> {
            AssociarMinistranteDto dto = new AssociarMinistranteDto();
            dto.setIdServidor(sm.getIdServidor());
            dto.setNome(sm.getNomeMinistrante());
            dto.setEmail(sm.getEmail());
            dto.setPapelId(sm.getPapelMinistrante().getId());
            cursoService.addMinistranteToCurso(cursoSalvo.getId(), dto);
        });

        // Marcar como convertido (status 5 por exemplo)
        statusTramitacaoRepository.findById(5L).ifPresent(solicitacao::setStatus);
        solicitacaoRepository.save(solicitacao);
    }

    private void validarPropriedadeOuAdmin(SolicitacaoCurso solicitacao) {
        SecurityUtils.getAuthenticationToken().ifPresent(auth -> {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("cursos.analisar.solicitacoes") ||
                            a.getAuthority().equals("cursos.manter.cursos"));
            if (!isAdmin && !auth.getUsuario().getId().equals(solicitacao.getIdServidorInteressado())) {
                throw new AppException("Acesso negado: você não é o proprietário desta solicitação.");
            }
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
        solicitacao.setPublicosAlvo(publicoAlvoRepository.findAllById(dto.getIdsPublicoAlvo()));

        if (dto.getIdLocalEvento() != null) {
            solicitacao.setLocalEvento(localEventoRepository.findById(dto.getIdLocalEvento())
                    .orElseThrow(() -> new NotFoundException("Local do evento não encontrado")));
        } else {
            solicitacao.setLocalEvento(null);
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

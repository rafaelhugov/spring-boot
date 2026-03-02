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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // A solicitação normalmente começa com status "Em Análise" (1L) ou "Pendente"
        StatusTramitacao statusInicial = statusTramitacaoRepository.findById(1L)
                .orElseThrow(() -> new AppException("Status inicial de tramitação não encontrado no banco."));
        solicitacao.setStatus(statusInicial);

        // Salva a entidade primeiro para ter o ID
        SolicitacaoCurso solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        // 2. Gerar PDF do PGA (Baseado no modelo do MPPB)
        byte[] pdfGerado = reportService.gerarPdfSolicitacao(solicitacaoSalva);

        // 3. Enviar para o MPVirtual
        try {
            // Em um caso de uso real, idInteressado seria extraído do token do usuário
            // logado
            Long usuarioLogadoMock = dto.getMinistrantes() != null && !dto.getMinistrantes().isEmpty()
                    && dto.getMinistrantes().get(0).getIdServidorInterno() != null
                            ? dto.getMinistrantes().get(0).getIdServidorInterno()
                            : 12345L;

            MPVirtualProcessoResponseDto integracaoResponse = mpVirtualService.abrirProcessoCurso(
                    "Solicitação de Curso: " + solicitacaoSalva.getNomeCurso(),
                    dto.getObservacoesGerais() != null ? dto.getObservacoesGerais()
                            : "Solicitação gerada via sistema Cursos-Web",
                    usuarioLogadoMock,
                    pdfGerado);

            // 4. Atualizar o registro com os dados de rastreamento do MPVirtual
            solicitacaoSalva.setNumeroPgaMpVirtual(integracaoResponse.getNumeroProcesso());
            solicitacaoRepository.save(solicitacaoSalva); // Salva a atualização

        } catch (Exception e) {
            log.error("Erro no fluxo do MPVirtual. Revertendo solicitação.", e);
            throw new AppException(e, "Ocorreu um erro ao abrir o processo no MPVirtual: " + e.getMessage());
        }

        return mapToResponseDto(solicitacaoSalva);
    }

    private SolicitacaoCurso mapToEntity(SolicitacaoCursoRequestDto dto) {
        SolicitacaoCurso solicitacao = new SolicitacaoCurso();

        solicitacao.setNomeCurso(dto.getNomeCurso());
        solicitacao.setEstimativaParticipantes(dto.getEstimativaParticipantes());

        // Identificação (MOCK)
        solicitacao.setIdServidorInteressado(64601L); // Rafael por exemplo
        solicitacao.setIdOrgaoInteressado(1L);

        // Lookups
        Modalidade modalidade = modalidadeRepository.findById(dto.getIdModalidade())
                .orElseThrow(() -> new NotFoundException("Modalidade não encontrada"));
        solicitacao.setModalidade(modalidade);

        PublicoAlvo publicoAlvo = publicoAlvoRepository.findById(dto.getIdPublicoAlvo())
                .orElseThrow(() -> new NotFoundException("Público-alvo não encontrado"));
        solicitacao.setPublicoAlvo(publicoAlvo);

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
                agenda.setDataEvento(agendaDto.getDataEvento());
                agenda.setHorarioInicio(agendaDto.getHorarioInicio());
                agenda.setHorarioFim(agendaDto.getHorarioFim());
                solicitacao.addAgenda(agenda);
            });
        }

        // Filhos: Ministrantes
        if (dto.getMinistrantes() != null) {
            dto.getMinistrantes().forEach(minDto -> {
                SolicitacaoMinistrante ministrante = new SolicitacaoMinistrante();
                ministrante.setIdServidorInterno(minDto.getIdServidorInterno());
                ministrante.setNomeMinistrante(minDto.getNomeMinistrante());
                ministrante.setContatoInfo(minDto.getContatoInfo());
                ministrante.setResumoCurriculo(minDto.getResumoCurriculo());
                ministrante.setIdArquivoCurriculoGed(minDto.getIdArquivoCurriculoGed());
                ministrante.setIdArquivoFotoGed(minDto.getIdArquivoFotoGed());

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
            dto.setModalidadeDescricao(entity.getModalidade().getDescricao());
        }
        if (entity.getPublicoAlvo() != null) {
            dto.setPublicoAlvoDescricao(entity.getPublicoAlvo().getNome());
        }

        dto.setDataCriacao(entity.getDataCriacao());
        dto.setDataModificacao(entity.getDataModificacao());

        return dto;
    }

    public SolicitacaoCursoResponseDto buscarPorId(Long id) {
        SolicitacaoCurso solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));
        return mapToResponseDto(solicitacao);
    }
}

package mppb.restapi.mobile.feature.mensagem.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.mensagem.dto.AcompanhamentoMensagemResponse;
import mppb.restapi.mobile.feature.mensagem.dto.CriarMensagemRequest;
import mppb.restapi.mobile.feature.mensagem.dto.CriteriosDestinatariosDto;
import mppb.restapi.mobile.feature.mensagem.dto.DestinatarioStatusResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemAnexoResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemEnviadaResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagensEnviadasPageResponse;
import mppb.restapi.mobile.feature.mensagem.dto.PreviaAlcanceRequest;
import mppb.restapi.mobile.feature.mensagem.dto.PreviaAlcanceResponse;
import mppb.restapi.mobile.feature.mensagem.entity.GrupoMensagemMembro;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAviso;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.StatusPublicacao;
import mppb.restapi.mobile.feature.mensagem.repository.GrupoMensagemMembroRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAnexoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAvisoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository.DestinatarioRh;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;

@Service
@RequiredArgsConstructor
public class MensagemEnvioService {

    private final MensagemAvisoRepository mensagemAvisoRepository;
    private final MensagemDestinatarioRepository mensagemDestinatarioRepository;
    private final MensagemAnexoRepository mensagemAnexoRepository;
    private final GrupoMensagemMembroRepository grupoMensagemMembroRepository;
    private final RhDestinatarioRepository rhDestinatarioRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final MensagemPushService mensagemPushService;
    private final ObjectMapper objectMapper;

    public PreviaAlcanceResponse previaAlcance(PreviaAlcanceRequest request) {
        if (request == null || request.modalidade() == null) {
            throw new IllegalArgumentException("A modalidade de envio é obrigatória.");
        }
        boolean remetenteUat = mppb.restapi.mobile.core.security.AutenticacaoAtual.login()
                .flatMap(IdentidadeSintetica::porLogin)
                .isPresent();
        List<DestinatarioRh> destinatarios = resolverDestinatariosComUat(
                request.modalidade(), request.criterios(), remetenteUat);
        return new PreviaAlcanceResponse(destinatarios.size());
    }

    @Transactional
    public MensagemEnviadaResponse enviarOuAgendar(String loginRemetente, CriarMensagemRequest request) {
        boolean temTexto = request.conteudo() != null && !request.conteudo().isBlank();
        boolean temAnexos = request.anexoIds() != null && !request.anexoIds().isEmpty();
        if (!temTexto && !temAnexos) {
            throw new IllegalArgumentException("Informe um texto ou ao menos um anexo/mídia para enviar o aviso.");
        }

        var sintetica = IdentidadeSintetica.porLogin(loginRemetente);
        PerfilUsuarioDto perfil = sintetica.map(id -> {
            PerfilUsuarioDto dto = new PerfilUsuarioDto();
            dto.setLogin(id.getLogin());
            dto.setMatricula(id.getMatricula());
            dto.setNome(id.getNome());
            return dto;
        }).orElseGet(() -> perfilUsuarioRepository.buscarPorLogin(loginRemetente));

        if (perfil == null || perfil.getMatricula() == null || perfil.getMatricula().isBlank()) {
            throw new IllegalStateException("Não foi possível identificar a matrícula do remetente autenticado.");
        }

        LocalDateTime agora = LocalDateTime.now();
        if (request.dataExpiracao() != null && !request.dataExpiracao().isAfter(agora)) {
            throw new IllegalArgumentException("A data de expiração deve ser futura.");
        }

        boolean remetenteUat = sintetica.isPresent();
        boolean agendar = request.dataAgendamento() != null && request.dataAgendamento().isAfter(agora);
        List<DestinatarioRh> destinatarios = agendar
                ? List.of()
                : resolverDestinatariosComUat(request.modalidade(), request.criterios(), remetenteUat);

        if (!agendar && destinatarios.isEmpty()) {
            throw new IllegalArgumentException("Nenhum destinatário ativo foi encontrado para os critérios selecionados.");
        }

        MensagemAviso aviso = new MensagemAviso();
        aviso.setTitulo(normalizarOpcional(request.titulo()));
        aviso.setConteudo(normalizarOpcional(request.conteudo()));
        aviso.setPrioridade(request.prioridade());
        aviso.setModalidade(request.modalidade());
        aviso.setExigeCiencia(request.exigeCiencia());
        aviso.setStatusPublicacao(agendar ? StatusPublicacao.AGENDADA : StatusPublicacao.ENVIADA);
        aviso.setCriteriosJson(serializarCriterios(request.criterios()));
        aviso.setRemetenteMatricula(perfil.getMatricula());
        aviso.setRemetenteNome(perfil.getNome() != null ? perfil.getNome() : loginRemetente);
        aviso.setDataAgendamento(request.dataAgendamento());
        aviso.setDataEnvio(agendar ? null : agora);
        aviso.setDataExpiracao(request.dataExpiracao());
        aviso = mensagemAvisoRepository.save(aviso);

        List<MensagemAnexo> anexos = vincularAnexos(aviso.getId(), request.anexoIds());

        if (!agendar) {
            publicarParaDestinatarios(aviso, anexos, destinatarios);
        }

        return montarResumo(aviso, anexos);
    }

    @Transactional
    public int processarAgendadas() {
        List<MensagemAviso> pendentes = mensagemAvisoRepository
                .findByStatusPublicacaoAndDataAgendamentoLessThanEqual(StatusPublicacao.AGENDADA, LocalDateTime.now());
        int processadas = 0;
        for (MensagemAviso aviso : pendentes) {
            CriteriosDestinatariosDto criterios = deserializarCriterios(aviso.getCriteriosJson());
            List<DestinatarioRh> destinatarios = resolverDestinatarios(aviso.getModalidade(), criterios);
            aviso.setStatusPublicacao(StatusPublicacao.ENVIADA);
            aviso.setDataEnvio(LocalDateTime.now());
            mensagemAvisoRepository.save(aviso);

            List<MensagemAnexo> anexos = mensagemAnexoRepository.findByMensagemIdOrderByIdAsc(aviso.getId());
            if (!destinatarios.isEmpty()) {
                publicarParaDestinatarios(aviso, anexos, destinatarios);
            }
            processadas++;
        }
        return processadas;
    }

    @Transactional
    public void cancelarAgendada(Long id) {
        MensagemAviso aviso = mensagemAvisoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada."));
        if (aviso.getStatusPublicacao() != StatusPublicacao.AGENDADA) {
            throw new IllegalStateException("Somente mensagens agendadas podem ser canceladas.");
        }
        aviso.setStatusPublicacao(StatusPublicacao.CANCELADA);
        mensagemAvisoRepository.save(aviso);
    }

    @Transactional
    public void arquivarRemetente(Long id, boolean arquivada) {
        MensagemAviso aviso = mensagemAvisoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada."));
        aviso.setArquivadaRemetente(arquivada);
        mensagemAvisoRepository.save(aviso);
    }

    private void publicarParaDestinatarios(
            MensagemAviso aviso,
            List<MensagemAnexo> anexos,
            List<DestinatarioRh> destinatarios) {

        List<MensagemDestinatario> registros = new ArrayList<>(destinatarios.size());
        List<String> matriculas = new ArrayList<>(destinatarios.size());

        for (DestinatarioRh item : destinatarios) {
            MensagemDestinatario dest = new MensagemDestinatario();
            dest.setMensagemId(aviso.getId());
            dest.setDestinatarioMatricula(item.matricula());
            dest.setDestinatarioNome(item.nome());
            dest.setDestinatarioSetor(item.setorNome());
            dest.setStatus(StatusDestinatario.PENDENTE);
            registros.add(dest);
            matriculas.add(item.matricula());
        }

        mensagemDestinatarioRepository.saveAll(registros);
        mensagemPushService.notificarDestinatarios(aviso, anexos, matriculas);
    }

    private List<DestinatarioRh> resolverDestinatarios(ModalidadeEnvio modalidade, CriteriosDestinatariosDto criterios) {
        if (modalidade == ModalidadeEnvio.BROADCAST) {
            return rhDestinatarioRepository.listarTodosAtivos();
        }
        if (criterios == null) {
            return List.of();
        }
        if (modalidade == ModalidadeEnvio.UNICAST) {
            return rhDestinatarioRepository.listarPorCriterios(null, null, null, null, criterios.matriculas());
        }

        Set<String> matriculasCombinadas = new LinkedHashSet<>();
        if (criterios.matriculas() != null) {
            matriculasCombinadas.addAll(criterios.matriculas());
        }
        if (criterios.grupoPersonalizadoIds() != null) {
            for (Long grupoId : criterios.grupoPersonalizadoIds()) {
                for (GrupoMensagemMembro membro : grupoMensagemMembroRepository.findByGrupoId(grupoId)) {
                    matriculasCombinadas.add(membro.getMatricula());
                }
            }
        }
        return rhDestinatarioRepository.listarPorCriterios(
                criterios.tiposCargo(),
                criterios.orgaoIds(),
                criterios.setorIds(),
                criterios.cargoIds(),
                new ArrayList<>(matriculasCombinadas));
    }

    private List<DestinatarioRh> resolverDestinatariosComUat(
            ModalidadeEnvio modalidade,
            CriteriosDestinatariosDto criterios,
            boolean incluirUatEmBroadcastEMulticast) {

        List<DestinatarioRh> base = new ArrayList<>(resolverDestinatarios(modalidade, criterios));
        List<DestinatarioRh> candidatosUat = List.of(
                new DestinatarioRh(
                        IdentidadeSintetica.UAT_MEMBRO.getMatricula(),
                        IdentidadeSintetica.UAT_MEMBRO.getNome(),
                        "1ª Promotoria de Justiça (UAT)",
                        "Promotor de Justiça",
                        "M"),
                new DestinatarioRh(
                        IdentidadeSintetica.UAT_SERVIDOR.getMatricula(),
                        IdentidadeSintetica.UAT_SERVIDOR.getNome(),
                        "Diretoria de Tecnologia da Informação (UAT)",
                        "Analista Ministerial",
                        "S"));

        Set<String> jaIncluidos = new LinkedHashSet<>();
        for (DestinatarioRh d : base) {
            jaIncluidos.add(d.matricula());
        }

        Set<String> matriculasSolicitadas = new LinkedHashSet<>();
        if (criterios != null) {
            if (criterios.matriculas() != null) {
                matriculasSolicitadas.addAll(criterios.matriculas());
            }
            if (criterios.grupoPersonalizadoIds() != null) {
                for (Long grupoId : criterios.grupoPersonalizadoIds()) {
                    for (GrupoMensagemMembro m : grupoMensagemMembroRepository.findByGrupoId(grupoId)) {
                        matriculasSolicitadas.add(m.getMatricula());
                    }
                }
            }
        }

        for (DestinatarioRh uat : candidatosUat) {
            if (jaIncluidos.contains(uat.matricula())) {
                continue;
            }
            boolean elegivel = false;
            if (modalidade == ModalidadeEnvio.BROADCAST && incluirUatEmBroadcastEMulticast) {
                elegivel = true;
            } else if (modalidade == ModalidadeEnvio.UNICAST) {
                elegivel = matriculasSolicitadas.contains(uat.matricula());
            } else if (modalidade == ModalidadeEnvio.MULTICAST) {
                boolean porMatricula = matriculasSolicitadas.contains(uat.matricula());
                boolean porTipo = incluirUatEmBroadcastEMulticast
                        && criterios != null
                        && criterios.tiposCargo() != null
                        && criterios.tiposCargo().stream().anyMatch(t -> uat.tipoCargo().equalsIgnoreCase(t));
                elegivel = porMatricula || porTipo;
            }
            if (elegivel) {
                base.add(uat);
                jaIncluidos.add(uat.matricula());
            }
        }
        return base;
    }

    private List<MensagemAnexo> vincularAnexos(Long mensagemId, List<Long> anexoIds) {
        if (anexoIds == null || anexoIds.isEmpty()) {
            return List.of();
        }
        List<MensagemAnexo> anexos = mensagemAnexoRepository.findByIdIn(anexoIds);
        for (MensagemAnexo anexo : anexos) {
            anexo.setMensagemId(mensagemId);
        }
        return mensagemAnexoRepository.saveAll(anexos);
    }

    public MensagemEnviadaResponse montarResumo(MensagemAviso aviso, List<MensagemAnexo> anexos) {
        long total = mensagemDestinatarioRepository.countByMensagemId(aviso.getId());
        long entregues = mensagemDestinatarioRepository.countByMensagemIdAndStatus(aviso.getId(), StatusDestinatario.ENTREGUE);
        long lidos = mensagemDestinatarioRepository.countByMensagemIdAndStatus(aviso.getId(), StatusDestinatario.LIDO);
        long cientes = mensagemDestinatarioRepository.countByMensagemIdAndDataCienciaIsNotNull(aviso.getId());
        long pendentes = Math.max(0, total - entregues - lidos);

        return new MensagemEnviadaResponse(
                aviso.getId(),
                aviso.getTitulo(),
                aviso.getConteudo(),
                aviso.getPrioridade(),
                aviso.getModalidade(),
                aviso.isExigeCiencia(),
                aviso.getStatusPublicacao(),
                aviso.getDataAgendamento(),
                aviso.getDataEnvio(),
                aviso.getDataExpiracao(),
                aviso.isArquivadaRemetente(),
                total,
                entregues + lidos,
                lidos,
                cientes,
                pendentes,
                anexos.stream().map(MensagemAnexoResponse::from).toList());
    }

    private String normalizarOpcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private String serializarCriterios(CriteriosDestinatariosDto criterios) {
        if (criterios == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(criterios);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Critérios de destinatários inválidos.", e);
        }
    }

    private CriteriosDestinatariosDto deserializarCriterios(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, CriteriosDestinatariosDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao ler critérios da mensagem agendada.", e);
        }
    }
}


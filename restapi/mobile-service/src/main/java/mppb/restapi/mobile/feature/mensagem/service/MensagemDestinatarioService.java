package mppb.restapi.mobile.feature.mensagem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.mensagem.dto.CaixaEntradaResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemAnexoResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemRecebidaResponse;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAviso;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAnexoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAvisoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemDestinatarioRepository;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;

@Service
@RequiredArgsConstructor
public class MensagemDestinatarioService {

    private final MensagemDestinatarioRepository destinatarioRepository;
    private final MensagemAvisoRepository avisoRepository;
    private final MensagemAnexoRepository anexoRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;

    @Transactional(readOnly = true)
    public CaixaEntradaResponse listarCaixaEntrada(String login, String filtro, int pagina, int tamanho) {
        String matricula = resolverMatriculaObrigatoria(login);
        String filtroNorm = filtro == null ? "TODAS" : filtro.trim().toUpperCase();
        boolean arquivada = "ARQUIVADAS".equals(filtroNorm);
        boolean somenteNaoLidas = "NAO_LIDAS".equals(filtroNorm);

        Page<MensagemDestinatario> page = destinatarioRepository.listarCaixaEntrada(
                matricula,
                arquivada,
                somenteNaoLidas,
                arquivada,
                PageRequest.of(Math.max(0, pagina), Math.max(1, Math.min(tamanho, 50))));

        List<Long> ids = page.getContent().stream().map(MensagemDestinatario::getMensagemId).toList();
        Map<Long, MensagemAviso> avisosPorId = ids.isEmpty()
                ? Map.of()
                : avisoRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(MensagemAviso::getId, Function.identity()));

        List<MensagemRecebidaResponse> itens = page.getContent().stream()
                .map(dest -> toResponse(avisosPorId.get(dest.getMensagemId()), dest))
                .toList();

        long naoLidas = destinatarioRepository.contarNaoLidasAtivas(matricula);
        return new CaixaEntradaResponse(
                itens,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                naoLidas);
    }

    @Transactional
    public void confirmarEntregaEmLote(String login, List<Long> mensagemIds) {
        if (mensagemIds == null || mensagemIds.isEmpty()) {
            return;
        }
        String matricula = resolverMatriculaObrigatoria(login);
        List<MensagemDestinatario> registros = destinatarioRepository
                .findByDestinatarioMatriculaAndMensagemIdIn(matricula, mensagemIds);

        LocalDateTime agora = LocalDateTime.now();
        boolean alterou = false;
        for (MensagemDestinatario dest : registros) {
            if (dest.getDataEntrega() == null) {
                dest.setDataEntrega(agora);
                if (dest.getStatus() == StatusDestinatario.PENDENTE) {
                    dest.setStatus(StatusDestinatario.ENTREGUE);
                }
                alterou = true;
            }
        }
        if (alterou) {
            destinatarioRepository.saveAll(registros);
        }
    }

    @Transactional
    public MensagemRecebidaResponse abrirMensagem(String login, Long mensagemId) {
        String matricula = resolverMatriculaObrigatoria(login);
        MensagemDestinatario dest = destinatarioRepository
                .findByMensagemIdAndDestinatarioMatricula(mensagemId, matricula)
                .orElseThrow(() -> new AccessDeniedException("Mensagem não pertence ao usuário autenticado."));

        MensagemAviso aviso = avisoRepository.findById(mensagemId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada."));

        LocalDateTime agora = LocalDateTime.now();
        boolean alterou = false;
        if (dest.getDataEntrega() == null) {
            dest.setDataEntrega(agora);
            alterou = true;
        }
        if (dest.getDataLeitura() == null) {
            dest.setDataLeitura(agora);
            dest.setStatus(StatusDestinatario.LIDO);
            alterou = true;
        }
        if (alterou) {
            destinatarioRepository.save(dest);
        }
        return toResponse(aviso, dest);
    }

    @Transactional
    public MensagemRecebidaResponse confirmarCiencia(String login, Long mensagemId) {
        String matricula = resolverMatriculaObrigatoria(login);
        MensagemDestinatario dest = destinatarioRepository
                .findByMensagemIdAndDestinatarioMatricula(mensagemId, matricula)
                .orElseThrow(() -> new AccessDeniedException("Mensagem não pertence ao usuário autenticado."));

        MensagemAviso aviso = avisoRepository.findById(mensagemId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada."));

        if (!aviso.isExigeCiencia()) {
            throw new IllegalStateException("Esta mensagem não exige confirmação de ciência.");
        }

        LocalDateTime agora = LocalDateTime.now();
        boolean alterou = false;
        if (dest.getDataEntrega() == null) {
            dest.setDataEntrega(agora);
            alterou = true;
        }
        if (dest.getDataLeitura() == null) {
            dest.setDataLeitura(agora);
            dest.setStatus(StatusDestinatario.LIDO);
            alterou = true;
        }
        if (dest.getDataCiencia() == null) {
            dest.setDataCiencia(agora);
            alterou = true;
        }
        if (alterou) {
            destinatarioRepository.save(dest);
        }
        return toResponse(aviso, dest);
    }

    @Transactional
    public void arquivar(String login, Long mensagemId, boolean arquivada) {
        String matricula = resolverMatriculaObrigatoria(login);
        MensagemDestinatario dest = destinatarioRepository
                .findByMensagemIdAndDestinatarioMatricula(mensagemId, matricula)
                .orElseThrow(() -> new AccessDeniedException("Mensagem não pertence ao usuário autenticado."));
        dest.setArquivadaDestinatario(arquivada);
        destinatarioRepository.save(dest);
    }

    @Transactional(readOnly = true)
    public MensagemAnexo buscarAnexoAutorizado(String login, boolean isAdmin, Long mensagemId, Long anexoId) {
        if (!isAdmin) {
            String matricula = resolverMatriculaObrigatoria(login);
            boolean vinculado = destinatarioRepository.existsByMensagemIdAndDestinatarioMatricula(mensagemId, matricula);
            if (!vinculado) {
                throw new AccessDeniedException("Acesso negado ao anexo solicitado.");
            }
        }
        return anexoRepository.findByIdAndMensagemId(anexoId, mensagemId)
                .orElseThrow(() -> new IllegalArgumentException("Anexo não encontrado para esta mensagem."));
    }

    private MensagemRecebidaResponse toResponse(MensagemAviso aviso, MensagemDestinatario dest) {
        List<MensagemAnexoResponse> anexos = anexoRepository.findByMensagemIdOrderByIdAsc(dest.getMensagemId())
                .stream()
                .map(MensagemAnexoResponse::from)
                .toList();

        return new MensagemRecebidaResponse(
                dest.getMensagemId(),
                aviso != null ? aviso.getTitulo() : null,
                aviso != null ? aviso.getConteudo() : null,
                aviso != null ? aviso.getPrioridade() : null,
                aviso != null ? aviso.getModalidade() : null,
                aviso != null && aviso.isExigeCiencia(),
                aviso != null ? aviso.getRemetenteNome() : null,
                aviso != null ? aviso.getDataEnvio() : null,
                aviso != null ? aviso.getDataExpiracao() : null,
                dest.getStatus(),
                dest.getDataEntrega(),
                dest.getDataLeitura(),
                dest.getDataCiencia(),
                dest.isArquivadaDestinatario(),
                anexos);
    }

    private String resolverMatriculaObrigatoria(String login) {
        PerfilUsuarioDto perfil = perfilUsuarioRepository.buscarPorLogin(login);
        if (perfil == null || perfil.getMatricula() == null || perfil.getMatricula().isBlank()) {
            throw new IllegalStateException("Matrícula do usuário autenticado não localizada.");
        }
        return perfil.getMatricula();
    }
}


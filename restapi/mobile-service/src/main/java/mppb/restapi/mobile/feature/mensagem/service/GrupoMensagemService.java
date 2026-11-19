package mppb.restapi.mobile.feature.mensagem.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.mensagem.dto.CatalogoGruposRhResponse;
import mppb.restapi.mobile.feature.mensagem.dto.DestinatarioRhResponse;
import mppb.restapi.mobile.feature.mensagem.dto.GrupoPersonalizadoRequest;
import mppb.restapi.mobile.feature.mensagem.dto.GrupoPersonalizadoResponse;
import mppb.restapi.mobile.feature.mensagem.entity.GrupoMensagem;
import mppb.restapi.mobile.feature.mensagem.entity.GrupoMensagemMembro;
import mppb.restapi.mobile.feature.mensagem.repository.GrupoMensagemMembroRepository;
import mppb.restapi.mobile.feature.mensagem.repository.GrupoMensagemRepository;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;

@Service
@RequiredArgsConstructor
public class GrupoMensagemService {

    private final RhDestinatarioRepository rhDestinatarioRepository;
    private final GrupoMensagemRepository grupoRepository;
    private final GrupoMensagemMembroRepository membroRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;

    @Transactional(readOnly = true)
    public CatalogoGruposRhResponse catalogoRh() {
        return new CatalogoGruposRhResponse(
                rhDestinatarioRepository.listarTiposCargo(),
                rhDestinatarioRepository.listarOrgaos(),
                rhDestinatarioRepository.listarSetores(),
                rhDestinatarioRepository.listarCargos());
    }

    @Transactional(readOnly = true)
    public List<DestinatarioRhResponse> buscarDestinatarios(String termo, int limite) {
        return rhDestinatarioRepository.buscarPorTexto(termo, limite).stream()
                .map(DestinatarioRhResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GrupoPersonalizadoResponse> listarPersonalizados() {
        return grupoRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GrupoPersonalizadoResponse criarPersonalizado(String login, GrupoPersonalizadoRequest request) {
        PerfilUsuarioDto perfil = perfilUsuarioRepository.buscarPorLogin(login);
        String matriculaCriador = (perfil != null && perfil.getMatricula() != null && !perfil.getMatricula().isBlank())
                ? perfil.getMatricula()
                : login;

        GrupoMensagem grupo = new GrupoMensagem();
        grupo.setNome(request.nome().trim());
        grupo.setDescricao(request.descricao() == null ? null : request.descricao().trim());
        grupo.setAtivo(true);
        grupo.setCriadoPorMatricula(matriculaCriador);
        grupo = grupoRepository.save(grupo);

        salvarMembros(grupo.getId(), request.matriculas());
        return toResponse(grupo);
    }

    @Transactional
    public GrupoPersonalizadoResponse atualizarPersonalizado(Long id, GrupoPersonalizadoRequest request) {
        GrupoMensagem grupo = grupoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Grupo personalizado não encontrado."));
        grupo.setNome(request.nome().trim());
        grupo.setDescricao(request.descricao() == null ? null : request.descricao().trim());
        grupo = grupoRepository.save(grupo);

        membroRepository.deleteByGrupoId(grupo.getId());
        salvarMembros(grupo.getId(), request.matriculas());
        return toResponse(grupo);
    }

    @Transactional
    public void removerPersonalizado(Long id) {
        GrupoMensagem grupo = grupoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Grupo personalizado não encontrado."));
        grupo.setAtivo(false);
        grupoRepository.save(grupo);
    }

    private void salvarMembros(Long grupoId, List<String> matriculas) {
        if (matriculas == null || matriculas.isEmpty()) {
            return;
        }
        Set<String> unicas = new LinkedHashSet<>();
        for (String m : matriculas) {
            if (m != null && !m.isBlank()) {
                unicas.add(m.trim());
            }
        }
        List<GrupoMensagemMembro> entidades = unicas.stream().map(m -> {
            GrupoMensagemMembro membro = new GrupoMensagemMembro();
            membro.setGrupoId(grupoId);
            membro.setMatricula(m);
            return membro;
        }).toList();
        membroRepository.saveAll(entidades);
    }

    private GrupoPersonalizadoResponse toResponse(GrupoMensagem grupo) {
        List<String> matriculas = membroRepository.findByGrupoId(grupo.getId()).stream()
                .map(GrupoMensagemMembro::getMatricula)
                .toList();
        return new GrupoPersonalizadoResponse(
                grupo.getId(),
                grupo.getNome(),
                grupo.getDescricao(),
                grupo.isAtivo(),
                matriculas.size(),
                matriculas);
    }
}


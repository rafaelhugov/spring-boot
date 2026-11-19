package mppb.restapi.mobile.feature.mensagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import mppb.restapi.mobile.feature.mensagem.controller.GrupoMensagemController;
import mppb.restapi.mobile.feature.mensagem.dto.CatalogoGruposRhResponse;
import mppb.restapi.mobile.feature.mensagem.dto.DestinatarioRhResponse;
import mppb.restapi.mobile.feature.mensagem.dto.GrupoPersonalizadoRequest;
import mppb.restapi.mobile.feature.mensagem.dto.GrupoPersonalizadoResponse;
import mppb.restapi.mobile.feature.mensagem.entity.GrupoMensagem;
import mppb.restapi.mobile.feature.mensagem.entity.GrupoMensagemMembro;
import mppb.restapi.mobile.feature.mensagem.repository.GrupoMensagemMembroRepository;
import mppb.restapi.mobile.feature.mensagem.repository.GrupoMensagemRepository;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository.DestinatarioRh;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository.OpcaoGrupoRh;
import mppb.restapi.mobile.feature.mensagem.service.GrupoMensagemService;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;

class GrupoMensagemTest {

    private RhDestinatarioRepository rhRepo;
    private GrupoMensagemRepository grupoRepo;
    private GrupoMensagemMembroRepository membroRepo;
    private PerfilUsuarioRepository perfilRepo;
    private GrupoMensagemService service;

    @BeforeEach
    void setUp() {
        rhRepo = mock(RhDestinatarioRepository.class);
        grupoRepo = mock(GrupoMensagemRepository.class);
        membroRepo = mock(GrupoMensagemMembroRepository.class);
        perfilRepo = mock(PerfilUsuarioRepository.class);
        service = new GrupoMensagemService(rhRepo, grupoRepo, membroRepo, perfilRepo);
    }

    @Test
    @DisplayName("Deve exigir anotação @PreAuthorize no controller de grupos")
    void deveExigirPreAuthorizeNoController() {
        PreAuthorize annotation = GrupoMensagemController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("authorityAdmin"));
    }

    @Test
    @DisplayName("Deve retornar catálogo dinâmico de RH e buscar destinatários por texto")
    void deveRetornarCatalogoRhEBuscarDestinatarios() {
        when(rhRepo.listarTiposCargo()).thenReturn(List.of(new OpcaoGrupoRh("S", "Servidor", 120)));
        when(rhRepo.listarOrgaos()).thenReturn(List.of(new OpcaoGrupoRh("1", "PGJ", 45)));
        when(rhRepo.listarSetores()).thenReturn(List.of(new OpcaoGrupoRh("10", "DTI", 18)));
        when(rhRepo.listarCargos()).thenReturn(List.of(new OpcaoGrupoRh("5", "Analista", 30)));

        CatalogoGruposRhResponse catalogo = service.catalogoRh();
        assertEquals(1, catalogo.tiposCargo().size());
        assertEquals("PGJ", catalogo.orgaos().get(0).nome());
        assertEquals("DTI", catalogo.setores().get(0).nome());
        assertEquals("Analista", catalogo.cargos().get(0).nome());

        when(rhRepo.buscarPorTexto("maria", 20)).thenReturn(List.of(
                new DestinatarioRh("2001", "Maria Silva", "DTI", "Analista", "S")));
        List<DestinatarioRhResponse> encontrados = service.buscarDestinatarios("maria", 20);
        assertEquals(1, encontrados.size());
        assertEquals("2001", encontrados.get(0).matricula());
    }

    @Test
    @DisplayName("Deve criar, atualizar, listar e remover logicamente grupo personalizado")
    void deveExecutarCrudDeGrupoPersonalizado() {
        PerfilUsuarioDto criador = new PerfilUsuarioDto();
        criador.setMatricula("1001");
        when(perfilRepo.buscarPorLogin("admin.msg")).thenReturn(criador);

        when(grupoRepo.save(any(GrupoMensagem.class))).thenAnswer(inv -> {
            GrupoMensagem g = inv.getArgument(0);
            if (g.getId() == null) {
                g.setId(7L);
            }
            return g;
        });

        GrupoMensagemMembro m1 = new GrupoMensagemMembro();
        m1.setGrupoId(7L);
        m1.setMatricula("2001");
        GrupoMensagemMembro m2 = new GrupoMensagemMembro();
        m2.setGrupoId(7L);
        m2.setMatricula("2002");
        when(membroRepo.findByGrupoId(7L)).thenReturn(List.of(m1, m2));

        GrupoPersonalizadoResponse criado = service.criarPersonalizado(
                "admin.msg",
                new GrupoPersonalizadoRequest(" Comitê TI ", " Equipe técnica ", List.of("2001", "2001", " 2002 ", "")));

        assertEquals(7L, criado.id());
        assertEquals("Comitê TI", criado.nome());
        assertEquals("Equipe técnica", criado.descricao());
        assertEquals(2, criado.quantidadeMembros());
        verify(membroRepo).saveAll(anyList());

        GrupoMensagem existente = new GrupoMensagem();
        existente.setId(7L);
        existente.setNome("Comitê TI");
        existente.setAtivo(true);
        when(grupoRepo.findByIdAndAtivoTrue(7L)).thenReturn(Optional.of(existente));

        when(grupoRepo.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(existente));
        assertEquals(1, service.listarPersonalizados().size());

        GrupoPersonalizadoResponse atualizado = service.atualizarPersonalizado(
                7L,
                new GrupoPersonalizadoRequest("Comitê TI v2", null, List.of("2001")));
        assertEquals("Comitê TI v2", atualizado.nome());
        verify(membroRepo).deleteByGrupoId(7L);

        service.removerPersonalizado(7L);
        assertFalse(existente.isAtivo());

        when(grupoRepo.findByIdAndAtivoTrue(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.removerPersonalizado(999L));
    }
}

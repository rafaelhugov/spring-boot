package mppb.restapi.common.infrastructure.security.util;

import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.FuncionalidadeAutorizadaResponseDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUtilsTest {

    @Test
    void addCursosDefaultPermissionsAdicionaPermissoesInternasParaCursosAcesso() {
        AcessoDataResponseDto acessoData = new AcessoDataResponseDto(
                null,
                new ArrayList<>(List.of(new FuncionalidadeAutorizadaResponseDto(638L,
                        "Acesso ao sistema de Cursos", "cursos.acesso"))),
                null,
                null
        );

        SecurityUtils.addCursosDefaultPermissions(acessoData);

        List<String> servicos = acessoData.getFuncionalidadesPermitidas().stream()
                .map(FuncionalidadeAutorizadaResponseDto::getServico)
                .toList();
        assertEquals(4, servicos.size());
        assertTrue(servicos.contains("cursos.acesso"));
        assertTrue(servicos.contains("cursos.dashboard"));
        assertTrue(servicos.contains("cursos.meus.cursos"));
        assertTrue(servicos.contains("cursos.solicitar"));
    }

    @Test
    void addCursosDefaultPermissionsNaoAdicionaSemCursosAcesso() {
        AcessoDataResponseDto acessoData = new AcessoDataResponseDto(
                null,
                new ArrayList<>(List.of(new FuncionalidadeAutorizadaResponseDto(1L,
                        "Outro acesso", "outro.acesso"))),
                null,
                null
        );

        SecurityUtils.addCursosDefaultPermissions(acessoData);

        assertEquals(1, acessoData.getFuncionalidadesPermitidas().size());
        assertEquals("outro.acesso", acessoData.getFuncionalidadesPermitidas().get(0).getServico());
    }
}

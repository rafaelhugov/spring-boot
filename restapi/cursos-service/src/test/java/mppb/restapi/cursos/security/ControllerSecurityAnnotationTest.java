package mppb.restapi.cursos.security;

import mppb.restapi.cursos.feature.common.controller.ArquivoController;
import mppb.restapi.cursos.feature.curso.controller.CertificadoController;
import mppb.restapi.cursos.feature.curso.controller.CursoController;
import mppb.restapi.cursos.feature.dashboard.controller.DashboardController;
import mppb.restapi.cursos.feature.documentos.controller.DocumentosController;
import mppb.restapi.cursos.feature.inscricao.controller.InscricaoInternoController;
import mppb.restapi.cursos.feature.publico.controller.PublicController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerSecurityAnnotationTest {

    @Test
    void cursoControllerDeveExigirPermissaoCeafEmNivelDeClasse() {
        PreAuthorize preAuthorize = CursoController.class.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasAuthority('cursos.manter.cursos')", preAuthorize.value());
    }

    @Test
    void dashboardControllerDeveExigirPermissaoDashboardEmNivelDeClasse() {
        PreAuthorize preAuthorize = DashboardController.class.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasAuthority('cursos.dashboard')", preAuthorize.value());
    }

    @Test
    void inscricaoInternaDeveExigirPermissaoDeUsuarioInterno() throws Exception {
        assertMethodPreAuthorize(
                InscricaoInternoController.class,
                "inscreverInterno",
                "hasAnyAuthority('cursos.meus.cursos', 'cursos.solicitar')");
        assertMethodPreAuthorize(
                InscricaoInternoController.class,
                "meusCursos",
                "hasAuthority('cursos.meus.cursos')");
    }

    @Test
    void certificadosDevemExigirPermissaoCeaf() throws Exception {
        assertMethodPreAuthorize(
                CertificadoController.class,
                "uploadTemplate",
                "hasAuthority('cursos.manter.cursos')");
        assertMethodPreAuthorize(
                CertificadoController.class,
                "downloadCertificado",
                "hasAuthority('cursos.manter.cursos')");
        assertMethodPreAuthorize(
                CertificadoController.class,
                "downloadFlyerInscricao",
                "hasAuthority('cursos.manter.cursos')");
    }

    @Test
    void documentosGeradosDevemExigirPermissaoCeaf() throws Exception {
        assertMethodPreAuthorize(
                DocumentosController.class,
                "gerarDocumento",
                "hasAuthority('cursos.manter.cursos')");
    }

    @Test
    void arquivoGenericoDevePossuirPreAuthorizeExplicito() throws Exception {
        assertMethodPreAuthorize(
                ArquivoController.class,
                "upload",
                "hasAnyAuthority('cursos.solicitar', 'cursos.analisar.solicitacoes', 'cursos.manter.cursos')");
        assertMethodPreAuthorize(
                ArquivoController.class,
                "getArquivo",
                "hasAnyAuthority('cursos.meus.cursos', 'cursos.solicitar', 'cursos.analisar.solicitacoes', 'cursos.manter.cursos')");
    }

    @Test
    void endpointsPublicosNaoPossuemPreAuthorizePorDesenhoMasDownloadsPorIdExigemControleCompensatorio() throws Exception {
        assertNull(findMethod(PublicController.class, "getArquivoPublico").getAnnotation(PreAuthorize.class));
        assertFalse(PublicController.class.isAnnotationPresent(PreAuthorize.class));
    }

    private void assertMethodPreAuthorize(Class<?> controller, String methodName, String expectedExpression) throws Exception {
        PreAuthorize preAuthorize = findMethod(controller, methodName).getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains(expectedExpression.replace("hasAuthority(", "").replace(")", ""))
                || preAuthorize.value().equals(expectedExpression));
    }

    private Method findMethod(Class<?> controller, String methodName) throws NoSuchMethodException {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException(controller.getName() + "." + methodName);
    }
}

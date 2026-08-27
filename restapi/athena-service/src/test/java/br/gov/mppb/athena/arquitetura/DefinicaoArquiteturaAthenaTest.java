package br.gov.mppb.athena.arquitetura;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class DefinicaoArquiteturaAthenaTest {

    @Test
    void deveDefinirSeparacaoEntreInterfaceVueEServicoSpringBoot() {
        assertThat(DefinicaoArquiteturaAthena.FRONTEND_TECNOLOGIA).isEqualTo("Vue 3");
        assertThat(DefinicaoArquiteturaAthena.FRONTEND_DIRETORIO_LOGICO).isEqualTo("frontend");
        assertThat(DefinicaoArquiteturaAthena.BACKEND_TECNOLOGIA).isEqualTo("Java Spring Boot");
        assertThat(DefinicaoArquiteturaAthena.BACKEND_DIRETORIO_LOGICO).isEqualTo("restapi/athena-service");

        Set<CamadaAthena> camadasDefinidas = DefinicaoArquiteturaAthena.responsabilidades()
                .stream()
                .map(ResponsabilidadeCamada::camada)
                .collect(Collectors.toSet());

        assertThat(camadasDefinidas)
                .containsExactlyInAnyOrderElementsOf(EnumSet.allOf(CamadaAthena.class));
        assertThat(DefinicaoArquiteturaAthena.responsabilidades())
                .allSatisfy(responsabilidade -> {
                    assertThat(responsabilidade.pacote()).startsWith("br.gov.mppb.athena.");
                    assertThat(responsabilidade.responsabilidades()).hasSizeGreaterThanOrEqualTo(3);
                });
    }

    @Test
    void deveDescreverDependenciasSemAcoplamentoIndevidoEntreCamadas() {
        Set<DependenciaCamada> dependencias = DefinicaoArquiteturaAthena.dependenciasPermitidas();

        assertThat(dependencias)
                .contains(
                        new DependenciaCamada(CamadaAthena.API, CamadaAthena.APLICACAO),
                        new DependenciaCamada(CamadaAthena.APLICACAO, CamadaAthena.DOMINIO),
                        new DependenciaCamada(CamadaAthena.APLICACAO, CamadaAthena.PERSISTENCIA),
                        new DependenciaCamada(CamadaAthena.APLICACAO, CamadaAthena.INTEGRACOES),
                        new DependenciaCamada(CamadaAthena.APLICACAO, CamadaAthena.SEGURANCA)
                );

        assertThat(dependencias)
                .doesNotContain(
                        new DependenciaCamada(CamadaAthena.DOMINIO, CamadaAthena.API),
                        new DependenciaCamada(CamadaAthena.DOMINIO, CamadaAthena.PERSISTENCIA),
                        new DependenciaCamada(CamadaAthena.DOMINIO, CamadaAthena.INTEGRACOES),
                        new DependenciaCamada(CamadaAthena.DOMINIO, CamadaAthena.SEGURANCA),
                        new DependenciaCamada(CamadaAthena.PERSISTENCIA, CamadaAthena.API),
                        new DependenciaCamada(CamadaAthena.INTEGRACOES, CamadaAthena.API)
                );
    }

    @Test
    void servicoBackendNaoDeveConterProjetoFrontendAcoplado() throws Exception {
        Path raizServico = Path.of("").toAbsolutePath();

        assertThat(Files.exists(raizServico.resolve("src/main/frontend"))).isFalse();
        assertThat(Files.exists(raizServico.resolve("src/main/vue"))).isFalse();
        assertThat(Files.exists(raizServico.resolve("package.json"))).isFalse();
        assertThat(Files.exists(raizServico.resolve("vite.config.js"))).isFalse();
        assertThat(Files.exists(raizServico.resolve("vite.config.ts"))).isFalse();
    }
}

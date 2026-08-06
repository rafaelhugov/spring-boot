package br.gov.mppb.ncap.feature.entidade.integration;

import br.gov.mppb.ncap.feature.entidade.Entidade;
import br.gov.mppb.ncap.feature.entidade.EntidadeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sync_srv_test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS administrador\\\\;CREATE SCHEMA IF NOT EXISTS autorizacao\\\\;CREATE SCHEMA IF NOT EXISTS tj\\\\;CREATE SCHEMA IF NOT EXISTS ncap\\\\;SET SCHEMA_SEARCH_PATH ncap,public",
    "api.cnmp.base-url="
})
@ActiveProfiles({"test", "insecure"})
@Transactional
class EntidadeSincronizacaoServiceTest {

    @Autowired
    private EntidadeSincronizacaoService syncService;

    @Autowired
    private EntidadeRepository entidadeRepository;

    @Test
    void deveSincronizarUnidadesPreservandoAlcunhaEInativandoAusentes() {
        entidadeRepository.deleteAll();

        // 1. Cadastrar uma entidade previamente com descrição personalizada (descricaomppb)
        Entidade preExistente = Entidade.builder()
                .nomecnmp("1ª Delegacia Distrital - João Pessoa")
                .descricaomppb("Meu Apelido Customizado")
                .tipoentidade("POLICIA_CIVIL")
                .idComarca(1L)
                .municipio("João Pessoa")
                .ativa(true)
                .build();
        preExistente = entidadeRepository.save(preExistente);

        // 2. Cadastrar uma entidade local que sumiu da API do CNMP (deve ser inativada)
        Entidade sumida = Entidade.builder()
                .nomecnmp("Delegacia Antiga Inexistente no CNMP")
                .tipoentidade("POLICIA_CIVIL")
                .idComarca(9L)
                .municipio("Sousa")
                .ativa(true)
                .build();
        sumida = entidadeRepository.save(sumida);

        // Executa sync
        syncService.sincronizar();

        // Verifica se preExistente manteve a descrição personalizada e associou o idCnmp
        Optional<Entidade> posSyncPre = entidadeRepository.findById(preExistente.getId());
        assertThat(posSyncPre).isPresent();
        assertThat(posSyncPre.get().getDescricaomppb()).isEqualTo("Meu Apelido Customizado");
        assertThat(posSyncPre.get().getIdCnmp()).isEqualTo(1L);

        // Verifica se sumida foi inativada
        Optional<Entidade> posSyncSumida = entidadeRepository.findById(sumida.getId());
        assertThat(posSyncSumida).isPresent();
        assertThat(posSyncSumida.get().getAtiva()).isFalse();
    }
}

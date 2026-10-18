package mppb.restapi.mobile.feature.notificacao.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import mppb.restapi.mobile.feature.notificacao.model.PreferenciasNotificacao;
import mppb.restapi.mobile.feature.notificacao.repository.PreferenciasNotificacaoRepository;

class PreferenciasNotificacaoServiceTest {

    @Test
    void mantemTodasHabilitadasQuandoUsuarioAindaNaoConfigurou() {
        PreferenciasNotificacaoRepository repository = new FakeRepository(Optional.empty());
        PreferenciasNotificacaoService service = new PreferenciasNotificacaoService(repository);

        PreferenciasNotificacao preferencias = service.buscar("123");

        assertTrue(preferencias.noticias());
        assertTrue(preferencias.designacoes());
        assertTrue(preferencias.contracheques());
        assertTrue(preferencias.pagamentos());
        assertTrue(preferencias.afastamentos());
    }

    @Test
    void aplicaCategoriaCorretaAosEventosFuncionais() {
        PreferenciasNotificacaoRepository repository = new FakeRepository(Optional.of(
                new PreferenciasNotificacao(true, true, false, true, false)));
        PreferenciasNotificacaoService service = new PreferenciasNotificacaoService(repository);

        assertFalse(service.permiteEventoFuncional("123", "NOVO_CONTRACHEQUE"));
        assertTrue(service.permiteEventoFuncional("123", "NOVO_PAGAMENTO"));
        assertFalse(service.permiteEventoFuncional("123", "AFASTAMENTO_MOVIMENTACAO"));
        assertTrue(service.permiteEventoFuncional("123", "EVENTO_FUTURO"));
    }

    private static class FakeRepository extends PreferenciasNotificacaoRepository {
        private final Optional<PreferenciasNotificacao> resultado;

        private FakeRepository(Optional<PreferenciasNotificacao> resultado) {
            super(null);
            this.resultado = resultado;
        }

        @Override
        public Optional<PreferenciasNotificacao> buscar(String matricula) {
            return resultado;
        }
    }
}

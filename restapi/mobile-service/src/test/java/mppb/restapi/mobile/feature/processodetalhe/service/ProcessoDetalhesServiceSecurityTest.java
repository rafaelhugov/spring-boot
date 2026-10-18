package mppb.restapi.mobile.feature.processodetalhe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mppb.restapi.mobile.feature.captcha.service.CaptchaService;
import mppb.restapi.mobile.feature.processodetalhe.dto.InteressadoDTO;
import mppb.restapi.mobile.feature.processodetalhe.dto.MovimentoDTO;
import mppb.restapi.mobile.feature.processodetalhe.dto.ProcessoDetalheDTO;
import mppb.restapi.mobile.feature.processodetalhe.repository.ProcessoDetalhesRepository;

class ProcessoDetalhesServiceSecurityTest {

    private ProcessoDetalhesService service;
    private BlockingRepository repository;

    @BeforeEach
    void setUp() {
        service = new ProcessoDetalhesService();
        repository = new BlockingRepository();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "captchaService", new AcceptedCaptchaService());
    }

    @Test
    void processoSigilosoOuInexistenteNaoDisparaConsultasFilhas() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.obterDetalhesProcesso(987654321L, "token", "valor"));

        assertEquals("Processo não encontrado.", error.getMessage());
        assertEquals(0, repository.childQueries);
    }

    private static class AcceptedCaptchaService extends CaptchaService {
        @Override
        public boolean validateCaptcha(String token, String text) {
            return true;
        }
    }

    private static class BlockingRepository extends ProcessoDetalhesRepository {
        private int childQueries;

        @Override
        public ProcessoDetalheDTO findDetalhesProcesso(Long idProcesso) {
            return null;
        }

        @Override
        public List<InteressadoDTO> findInteressadosPorProcesso(Long idProcesso) {
            childQueries++;
            return List.of();
        }

        @Override
        public List<MovimentoDTO> findMovimentosPorProcesso(Long idProcesso) {
            childQueries++;
            return List.of();
        }
    }
}

package mppb.restapi.mobile.feature.processodetalhe.service;

import mppb.restapi.mobile.feature.captcha.service.CaptchaService;
import mppb.restapi.mobile.feature.processodetalhe.dto.ProcessoDetalheDTO;
import mppb.restapi.mobile.feature.processodetalhe.repository.ProcessoDetalhesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProcessoDetalhesService {

    @Autowired
    private ProcessoDetalhesRepository repository;

    @Autowired
    private CaptchaService captchaService;

    public ProcessoDetalheDTO obterDetalhesProcesso(Long idProcesso, String captchaToken, String captchaValue) {
        
        // 1. Validar Captcha
        if (!captchaService.validateCaptcha(captchaToken, captchaValue)) {
            throw new IllegalArgumentException("CAPTCHA inválido ou expirado.");
        }

        // 2. Buscar Cabeçalho
        ProcessoDetalheDTO detalhes = repository.findDetalhesProcesso(idProcesso);
        
        if (detalhes == null) {
            throw new IllegalArgumentException("Processo não encontrado.");
        }

        // 3. Preencher Filhos
        detalhes.setInteressados(repository.findInteressadosPorProcesso(idProcesso));
        detalhes.setMovimentos(repository.findMovimentosPorProcesso(idProcesso));

        return detalhes;
    }
}

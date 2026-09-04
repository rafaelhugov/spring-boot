package mppb.restapi.mobile.feature.noticias.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.noticias.model.NoticiaPublicada;
import mppb.restapi.mobile.feature.noticias.repository.NoticiaControleRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticiaDeteccaoService {

    private final NoticiaService noticiaService;
    private final NoticiaControleRepository controleRepository;

    public int detectarNovasNoticias() {
        List<NoticiaPublicada> noticias = noticiaService.getUltimasNoticiasPublicadas();
        if (noticias.isEmpty()) {
            return 0;
        }

        if (!controleRepository.possuiNoticiasConhecidas()) {
            int conhecidas = controleRepository.registrarConhecidasIniciais(noticias);
            log.info("Baseline inicial de noticias registrado: conhecidas={}", conhecidas);
            return 0;
        }

        int novas = 0;
        for (NoticiaPublicada noticia : noticias) {
            if (controleRepository.registrarNova(noticia)) {
                novas++;
            }
        }
        log.info("Deteccao de noticias finalizada: lidas={}, novas={}", noticias.size(), novas);
        return novas;
    }
}

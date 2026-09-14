package mppb.restapi.mobile.feature.noticias.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${noticias.notificacao.baseline-notificar-recentes-minutos:120}")
    private long baselineNotificarRecentesMinutos;

    public int detectarNovasNoticias() {
        List<NoticiaPublicada> noticias = noticiaService.getUltimasNoticiasPublicadas();
        if (noticias.isEmpty()) {
            return 0;
        }

        if (!controleRepository.possuiNoticiasConhecidas()) {
            return registrarBaselineInicial(noticias);
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

    private int registrarBaselineInicial(List<NoticiaPublicada> noticias) {
        LocalDateTime limiteRecentes = LocalDateTime.now().minusMinutes(baselineNotificarRecentesMinutos);
        List<NoticiaPublicada> recentes = noticias.stream()
                .filter(noticia -> isPublicacaoRecente(noticia, limiteRecentes))
                .toList();
        List<NoticiaPublicada> historicas = noticias.stream()
                .filter(noticia -> !isPublicacaoRecente(noticia, limiteRecentes))
                .toList();

        int conhecidas = controleRepository.registrarConhecidasIniciais(historicas);
        int novas = 0;
        for (NoticiaPublicada noticia : recentes) {
            if (controleRepository.registrarNova(noticia)) {
                novas++;
            }
        }

        log.info(
                "Baseline inicial de noticias registrado: lidas={}, historicas={}, recentes={}, novas={}",
                noticias.size(),
                conhecidas,
                recentes.size(),
                novas);
        return novas;
    }

    private boolean isPublicacaoRecente(NoticiaPublicada noticia, LocalDateTime limiteRecentes) {
        return noticia.dataPublicacao() != null && !noticia.dataPublicacao().isBefore(limiteRecentes);
    }
}

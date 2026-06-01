package mppb.restapi.cursos.feature.curso.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CursoConclusaoJob {

    private final CursoRepository cursoRepository;

    /**
     * Job executado diariamente à 00:01 (Horário de Brasília) para fechar automaticamente
     * os cursos cuja data de realização final já passou.
     */
    @Scheduled(cron = "0 1 0 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void verificarCursosConcluidos() {
        log.info("[CursoConclusaoJob] Iniciando verificação de cursos com período concluído...");
        
        int atualizados = cursoRepository.atualizarCursosConcluidos(LocalDateTime.now());
        
        if (atualizados > 0) {
            log.info("[CursoConclusaoJob] {} curso(s) atualizado(s) para o status FINALIZADO.", atualizados);
        } else {
            log.info("[CursoConclusaoJob] Nenhum curso precisou ser atualizado.");
        }
    }
}

package mppb.restapi.cursos.feature.solicitacao.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.cursos.feature.lookup.entities.StatusTramitacao;
import mppb.restapi.cursos.feature.lookup.repository.StatusTramitacaoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final StatusTramitacaoRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute(
                    "DO $$ " +
                            "BEGIN " +
                            "    IF NOT EXISTS (SELECT 1 FROM information_schema.columns " +
                            "                   WHERE table_schema='curso' AND table_name='solicitacaocurso' AND column_name='id_curso_gerado') THEN "
                            +
                            "        ALTER TABLE curso.solicitacaocurso ADD COLUMN id_curso_gerado bigint; " +
                            "    END IF; " +
                            "END $$;");
        } catch (Exception e) {
            log.warn("[DATA-LOADER] Erro ao verificar esquema: {}", e.getMessage());
        }

        seedStatus(3L, "DEFERIDO");
        seedStatus(4L, "INDEFERIDO");
        seedStatus(5L, "EM ANÁLISE");
        seedStatus(6L, "CONVERTIDO");
    }

    private void seedStatus(Long id, String descricao) {
        try {
            if (!repository.existsById(id)) {
                log.info("[DATA-LOADER] Inserindo status faltante: ID={}, Descrição={}", id, descricao);
                repository.save(new StatusTramitacao(id, descricao));
            }
        } catch (Exception e) {
            log.warn("[DATA-LOADER] Erro ao verificar ou inserir status {} (possível erro de permissão): {}", id, e.getMessage());
        }
    }
}

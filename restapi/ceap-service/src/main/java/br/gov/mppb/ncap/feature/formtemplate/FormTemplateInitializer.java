package br.gov.mppb.ncap.feature.formtemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormTemplateInitializer {

    private final FormTemplateRepository repository;
    private final FormTemplateSincronizacaoService syncService;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeTemplates() {
        log.info("Verificando a necessidade de sincronizar os templates de formulário do CNMP...");
        try {
            // Executa a sincronização de forma incondicional para garantir que templates específicos do NCAP sejam criados,
            // mesmo que a tabela já contenha templates legados de outros sistemas.
            syncService.sincronizarTemplates();

            // Dump detalhado de templates cadastrados
            List<Map<String, Object>> todosTemplates = jdbcTemplate.queryForList(
                    "SELECT id_formtemplate, resolucao, tipoentidade, ativo FROM ncap.formtemplates");

            // --- AUTO-HEALING: CORREÇÃO DE INSPEÇÕES SEM TEMPLATE ---
            log.info("Iniciando auto-healing para inspeções legadas sem template associado...");
            List<Map<String, Object>> inspecoesSemTemplate = jdbcTemplate.queryForList(
                    "SELECT i.id_inspecao, e.tipoentidade FROM ncap.inspecoes i " +
                    "JOIN ncap.entidades e ON i.id_entidade = e.id_entidade " +
                    "WHERE i.id_formtemplate IS NULL");

            for (Map<String, Object> inspecao : inspecoesSemTemplate) {
                Long idInspecao = ((Number) inspecao.get("id_inspecao")).longValue();
                String tipoEntidade = (String) inspecao.get("tipoentidade");

                log.info("Tentando associar template para inspeção ID: {} (Tipo de Unidade: {})", idInspecao, tipoEntidade);

                List<Map<String, Object>> templates = jdbcTemplate.queryForList(
                        "SELECT id_formtemplate FROM ncap.formtemplates " +
                        "WHERE tipoentidade = ? AND ativo = true " +
                        "ORDER BY criadoem DESC LIMIT 1", tipoEntidade);

                if (!templates.isEmpty()) {
                    Long idTemplate = ((Number) templates.get(0).get("id_formtemplate")).longValue();
                    log.info("Template encontrado (ID: {}). Associando à inspeção ID: {}", idTemplate, idInspecao);

                    // 1. Atualiza o id_formtemplate da inspeção
                    jdbcTemplate.update(
                            "UPDATE ncap.inspecoes SET id_formtemplate = ? WHERE id_inspecao = ?",
                            idTemplate, idInspecao);

                    // 2. Inicializa as respostas em branco
                    List<Map<String, Object>> perguntas = jdbcTemplate.queryForList(
                            "SELECT p.id_pergunta FROM ncap.perguntas p " +
                            "JOIN ncap.blocos b ON p.id_bloco = b.id_bloco " +
                            "JOIN ncap.secoes s ON b.id_secao = s.id_secao " +
                            "WHERE s.id_template = ?", idTemplate);

                    int respostasInseridas = 0;
                    for (Map<String, Object> pergunta : perguntas) {
                        Long idPergunta = ((Number) pergunta.get("id_pergunta")).longValue();
                        respostasInseridas += jdbcTemplate.update(
                                "INSERT INTO ncap.respostasinspecao (id_inspecao, id_pergunta) " +
                                "VALUES (?, ?) ON CONFLICT (id_inspecao, id_pergunta) DO NOTHING",
                                idInspecao, idPergunta);
                    }
                    log.info("Auto-healing concluído para inspeção ID: {}. {} respostas em branco inseridas/verificadas.", idInspecao, respostasInseridas);
                } else {
                    log.warn("Nenhum template ativo encontrado para o tipo de entidade '{}'. Não foi possível associar à inspeção ID: {}", tipoEntidade, idInspecao);
                }
            }

            // Dump das inspeções e tokens para target/tokens.txt
            log.info("Gerando dump de inspeções locais em target/tokens.txt...");
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT i.id_inspecao, i.id_formtemplate, i.status, i.tokenacesso, e.nomecnmp, e.tipoentidade " +
                    "FROM ncap.inspecoes i " +
                    "LEFT JOIN ncap.entidades e ON i.id_entidade = e.id_entidade");

            try (PrintWriter writer = new PrintWriter(new FileWriter("/home/marcelo/git/cnmp/ncap/backend/target/tokens.txt"))) {
                writer.println("=== DUMP DE TEMPLATES NCAP ===");
                for (Map<String, Object> t : todosTemplates) {
                    writer.printf("Template ID: %s | Resolução: %s | Tipo Entidade: %s | Ativo: %s%n",
                            t.get("id_formtemplate"),
                            t.get("resolucao"),
                            t.get("tipoentidade"),
                            t.get("ativo")
                    );
                }
                writer.println();
                writer.println("=== DUMP DE INSPECOES NCAP ===");
                for (Map<String, Object> row : rows) {
                    writer.printf("ID: %s | Unidade: %s (%s) | Template ID: %s | Status: %s | Token: %s%n",
                            row.get("id_inspecao"),
                            row.get("nomecnmp"),
                            row.get("tipoentidade"),
                            row.get("id_formtemplate"),
                            row.get("status"),
                            row.get("tokenacesso")
                    );
                }
                log.info("Dump de inspeções concluído com sucesso.");
            }
        } catch (Exception e) {
            log.error("Erro ao realizar a sincronização automática ou dump de templates", e);
        }
    }
}

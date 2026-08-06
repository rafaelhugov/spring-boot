package br.gov.mppb.ncap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@SpringBootTest
@ActiveProfiles({"test", "insecure"})
class NcapApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

    @Test
    void discoverDb() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("--- DB DETAIL START ---");
            String sql = "SELECT m.nome AS municipio, " +
                         "       COALESCE(c1.id, c2.id) AS id_comarca, " +
                         "       COALESCE(c1.nome, c2.nome) AS comarca " +
                         "FROM public.municipio m " +
                         "LEFT JOIN tj.comarca c1 ON m.id = c1.id_municipio AND c1.excluido = false " +
                         "LEFT JOIN tj.termo t ON m.id = t.id_municipio " +
                         "LEFT JOIN tj.comarca c2 ON t.id_comarca = c2.id AND c2.excluido = false " +
                         "WHERE m.nome ILIKE 'João Pessoa' OR m.nome ILIKE 'Cabedelo' OR m.nome ILIKE 'Campina Grande' OR m.nome ILIKE 'Patos' OR m.nome ILIKE 'Conde' " +
                         "ORDER BY m.nome";
            try (var stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    System.out.println("  municipio: " + rs.getString("municipio") + 
                                       " -> comarca: " + rs.getString("comarca") + 
                                       " (id: " + rs.getLong("id_comarca") + ")");
                }
            }
            System.out.println("--- DB DETAIL END ---");
        }
    }
}

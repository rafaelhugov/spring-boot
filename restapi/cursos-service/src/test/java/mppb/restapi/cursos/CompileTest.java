package mppb.restapi.cursos;

import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.api.Test;

public class CompileTest {
    @Test
    public void test() {
        try {
            JasperCompileManager.compileReport("src/main/resources/reports/lista_presenca.jrxml");
            System.out.println("SUCESSO!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

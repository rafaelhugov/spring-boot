package mppb.restapi.cursos;
import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.api.Test;
public class CompileReportTest {
    @Test public void compile() throws Exception {
        JasperCompileManager.compileReportToFile(
            "src/main/resources/reports/solicitacao_curso.jrxml",
            "src/main/resources/reports/solicitacao_curso.jasper");
        System.out.println("COMPILED OK");
    }
}

package mppb.restapi.cursos.feature.solicitacao.util;

import net.sf.jasperreports.engine.JasperCompileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;

/**
 * Small utility to compile all JRXML files under src/main/resources/reports
 * into target/classes/reports as .jasper files. Invoked by Maven (exec plugin)
 * during the build (process-classes phase).
 */
public class ReportCompiler {

    private static final Logger log = LoggerFactory.getLogger(ReportCompiler.class);

    public static void main(String[] args) {
        Path srcDir = Paths.get("src/main/resources/reports");
        Path outDir = Paths.get("target/classes/reports");

        try {
            if (!Files.exists(srcDir)) {
                log.info("Relatórios não encontrados em {} — nada a compilar.", srcDir.toAbsolutePath());
                return;
            }
            Files.createDirectories(outDir);

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(srcDir, "*.jrxml")) {
                for (Path p : ds) {
                    try {
                        String inPath = p.toAbsolutePath().toString();
                        String outName = p.getFileName().toString().replaceAll("\\.jrxml$", ".jasper");
                        Path outPath = outDir.resolve(outName);

                        // Prevent external XML resolution issues
                        try {
                            System.setProperty("javax.xml.accessExternalSchema", "");
                            System.setProperty("javax.xml.accessExternalDTD", "");
                        } catch (SecurityException se) {
                            log.warn("Não foi possível setar propriedades do parser XML: {}", se.getMessage());
                        }

                        log.info("Compilando {} -> {}", inPath, outPath.toAbsolutePath());
                        JasperCompileManager.compileReportToFile(inPath, outPath.toAbsolutePath().toString());
                        // also copy the compiled .jasper back to src/main/resources so
                        // the resources plugin will include it in the packaged JAR
                        try {
                            Path srcOut = srcDir.resolve(outName);
                            Files.copy(outPath, srcOut, StandardCopyOption.REPLACE_EXISTING);
                            log.info("Copiado .jasper para recursos fonte: {}", srcOut.toAbsolutePath());
                        } catch (Exception e) {
                            log.warn("Não foi possível copiar .jasper para src/main/resources: {}", e.getMessage());
                        }
                    } catch (Exception e) {
                        log.error("Erro compilando relatório {}: {}", p, e.getMessage(), e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Erro ao compilar relatórios: {}", e.getMessage(), e);
        }
    }
}

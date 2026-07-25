package mppb.restapi.cursos.feature.manual.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ManualPdfService {

    @Value("${app.manual.frontend-url:https://cursos.mppb.mp.br}")
    private String frontendUrl;

    @Value("${app.manual.node-engine-path}")
    private String nodeEnginePath;

    public byte[] gerarPdfManual(String tipo) throws IOException, InterruptedException {
        String fileName = "manual_" + tipo + "_" + UUID.randomUUID() + ".pdf";
        File tempFile = File.createTempFile("manual_", ".pdf");
        String outputPath = tempFile.getAbsolutePath();

        String route = tipo.equalsIgnoreCase("solicitante") ? "/manual/solicitante" : "/manual/administrativo";
        String fullUrl = frontendUrl + route;

        log.info("Iniciando geração de PDF para {} usando URL: {}", tipo, fullUrl);

        ProcessBuilder pb = new ProcessBuilder("node", nodeEnginePath, fullUrl, outputPath);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // Captura log do node para depuração
        String output = new String(process.getInputStream().readAllBytes());
        log.debug("Saída do engine PDF: {}", output);

        boolean finished = process.waitFor(2, TimeUnit.MINUTES);
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Tempo limite excedido ao gerar PDF do manual");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("Erro ao executar engine de PDF: " + output);
        }

        byte[] pdfBytes = Files.readAllBytes(tempFile.toPath());
        
        // Limpeza
        if (!tempFile.delete()) {
            log.warn("Não foi possível deletar o arquivo temporário: {}", outputPath);
        }

        return pdfBytes;
    }
}

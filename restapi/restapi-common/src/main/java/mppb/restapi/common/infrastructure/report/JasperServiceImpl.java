package mppb.restapi.common.infrastructure.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.core.util.IoUtil;
import net.sf.jasperreports.engine.*;
import org.springframework.stereotype.Service;

import mppb.restapi.common.core.error.NotFoundException;

import javax.sql.DataSource;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JasperServiceImpl implements JasperService {

    private static final String REPORT_PATH = "classpath:reports/";

    private final DataSource dataSource;

    @Override
    public byte[] gerarPdfConnection(String nomeArquivoJasper, Map<String, Object> parametros) {
        return gerarPdfDatasource(nomeArquivoJasper, parametros, null);
    }

    @Override
    public byte[] gerarPdfConnection(InputStream jasperStream, Map<String, Object> parametros) {
        return gerarPdfDatasource(jasperStream, parametros, null);
    }

    @Override
    public byte[] gerarPdfEmptyDatasource(InputStream jasperStream, Map<String, Object> params) {
        return gerarPdfDatasource(jasperStream, params, new JREmptyDataSource());
    }

    @Override
    public byte[] gerarPdfEmptyDatasource(String nomeArquivoJasper, Map<String, Object> params) {
        return gerarPdfDatasource(nomeArquivoJasper, params, new JREmptyDataSource());
    }

    @Override
    public byte[] gerarPdfDatasource(InputStream jasperStream, Map<String, Object> params, JRDataSource jrDataSource) {
        try {
            JasperPrint jasperPrint = preencherRelatorio(jasperStream, params, jrDataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (Exception e) {
            log.error("Erro ao gerar PDF: {}", e.getMessage(), e);
            throw new AppException(e);
        }
    }

    @Override
    public byte[] gerarPdfDatasource(String nomeArquivoJasper, Map<String, Object> params, JRDataSource jrDataSource) {
        String fullPath = REPORT_PATH + nomeArquivoJasper;
        log.info("Buscando relatório em: {}", fullPath);
        URL url = IoUtil.getResource(fullPath);

        if (url == null) {
            log.error("URL do relatório é nula para o caminho: {}", fullPath);
            throw new NotFoundException("Relatório não encontrado no classpath: {0}", new String[] { fullPath });
        }

        try (InputStream jasperStream = url.openStream()) {
            if (jasperStream == null) {
                log.error("InputStream do relatório é nulo para o caminho: {}", fullPath);
                throw new NotFoundException("Relatório não encontrado: {0}", new String[] { nomeArquivoJasper });
            }

            JasperPrint jasperPrint;
            if (nomeArquivoJasper.endsWith(".jrxml")) {
                log.info("Compilando relatório JRXML: {}", nomeArquivoJasper);
                // Evitar resolução externa de schemas/DTDs que pode falhar em ambientes restritos
                try {
                    System.setProperty("javax.xml.accessExternalSchema", "");
                    System.setProperty("javax.xml.accessExternalDTD", "");
                } catch (SecurityException se) {
                    log.warn("Não foi possível setar propriedades de XML parser: {}", se.getMessage());
                }

                JasperReport jasperReport = JasperCompileManager.compileReport(jasperStream);
                jasperPrint = preencherRelatorio(jasperReport, params, jrDataSource);
            } else {
                jasperPrint = preencherRelatorio(jasperStream, params, jrDataSource);
            }

            try {
                return JasperExportManager.exportReportToPdf(jasperPrint);
            } catch (JRException e) {
                throw new AppException(e);
            }

        } catch (Exception e) {
            log.error("CRITICAL ERROR in JasperServiceImpl for {}: {}", nomeArquivoJasper, e.getMessage(), e);
            e.printStackTrace();
            throw new AppException(e, "Erro ao gerar relatório", new String[] { e.getMessage() });
        }
    }

    private JasperPrint preencherRelatorio(JasperReport jasperReport, Map<String, Object> params,
            JRDataSource jrDataSource) throws Exception {
        if (jrDataSource != null) {
            return JasperFillManager.fillReport(jasperReport, params, jrDataSource);
        }
        try (Connection connection = dataSource.getConnection()) {
            return JasperFillManager.fillReport(jasperReport, params, connection);
        }
    }

    private JasperPrint preencherRelatorio(InputStream jasperStream, Map<String, Object> params,
            JRDataSource jrDataSource) throws Exception {
        if (jrDataSource != null) {
            return JasperFillManager.fillReport(jasperStream, params, jrDataSource);
        }
        try (Connection connection = dataSource.getConnection()) {
            return JasperFillManager.fillReport(jasperStream, params, connection);
        }
    }

}

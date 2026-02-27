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
    public byte[] gerarPdfConnection(String nomeArquivoJasper, Map<String, Object> parametros ) {
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
        JasperPrint jasperPrint = preencherRelatorio(jasperStream, params, jrDataSource);
        try {
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            throw new AppException(e);
        }
    }

    @Override
    public byte[] gerarPdfDatasource(String nomeArquivoJasper, Map<String, Object> params, JRDataSource jrDataSource) {

        URL url = IoUtil.getResource(REPORT_PATH + nomeArquivoJasper);

        try (InputStream jasperStream = url.openStream()) {

            if (jasperStream == null) {
                throw new NotFoundException("Relatório não encontrado: {0}", new String[]{nomeArquivoJasper});
            }

            return this.gerarPdfDatasource(jasperStream, params, jrDataSource);

        } catch (Exception e) {
            log.error("Erro ao gerar relatório: {}",  e);
            throw new AppException(e, "Erro ao gerar relatório", new String[]{e.getMessage()});
        }
    }

    private JasperPrint preencherRelatorio(InputStream jasperStream, Map<String, Object> params, JRDataSource jrDataSource) {
        try {
            if (jrDataSource != null) {
                return JasperFillManager.fillReport(jasperStream, params, jrDataSource);
            }
            try (Connection connection = dataSource.getConnection()) {
                return JasperFillManager.fillReport(jasperStream, params, connection);
            }
        } catch (Exception e) {
            throw new AppException(e);
        }
    }

}

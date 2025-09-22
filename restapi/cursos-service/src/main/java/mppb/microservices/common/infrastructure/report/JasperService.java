package mppb.microservices.common.infrastructure.report;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;

public interface JasperService {
    byte[] gerarPdfConnection(InputStream jasperStream, Map<String, Object> params) ;
    byte[] gerarPdfConnection(String nomeArquivoJasper, Map<String, Object> params);
    byte[] gerarPdfDatasource(String nomeArquivoJasper, Map<String, Object> params, JRDataSource jrDataSource);
    byte[] gerarPdfDatasource(InputStream jasperStream, Map<String, Object> params, JRDataSource jrDataSource);
    byte[] gerarPdfEmptyDatasource(String nomeArquivoJasper, Map<String, Object> params);
    byte[] gerarPdfEmptyDatasource(InputStream jasperStream, Map<String, Object> params);
}

package mppb.restapi.common.infrastructure.report;

import net.sf.jasperreports.engine.JRDataSource;

import java.io.InputStream;
import java.util.Map;

public interface JasperService {
    byte[] gerarPdfConnection(InputStream jasperStream, Map<String, Object> params) ;
    byte[] gerarPdfConnection(String nomeArquivoJasper, Map<String, Object> params);
    byte[] gerarPdfDatasource(String nomeArquivoJasper, Map<String, Object> params, JRDataSource jrDataSource);
    byte[] gerarPdfDatasource(InputStream jasperStream, Map<String, Object> params, JRDataSource jrDataSource);
    byte[] gerarPdfEmptyDatasource(String nomeArquivoJasper, Map<String, Object> params);
    byte[] gerarPdfEmptyDatasource(InputStream jasperStream, Map<String, Object> params);
}

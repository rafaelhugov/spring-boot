package mppb.restapi.common.infrastructure.service.mpvirtual;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualMetadataDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoResponseDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualCriarMinutaResponseDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualMovimentarRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MPVirtualIntegrationService {

        private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        @Value("${api.mpvirtual.url:http://10.128.4.14:8082}")
        private String mpvirtualUrl;

        public MPVirtualMetadataDto construirMetadata(String resumo, Integer idPessoa) {
                return MPVirtualMetadataDto.builder()
                                .idTipoDocumentoPecaInicial(132L) // Fixo: Solicitação de Curso
                                .codClasse(910020L) // Fixo
                                .codAssunto(930257L) // Fixo
                                .resumo(resumo)
                                .instancia("A")
                                .interessados(java.util.Arrays.asList(
                                                MPVirtualMetadataDto.InteressadoDto.builder()
                                                                .idTipoInteressado(72) // Fixo: Interessado
                                                                .idPessoa(idPessoa)
                                                                .sigiloso(false)
                                                                .build(),
                                                MPVirtualMetadataDto.InteressadoDto.builder()
                                                                .idTipoInteressado(1) // Fixo: Requerente
                                                                .idPessoa(idPessoa)
                                                                .sigiloso(false)
                                                                .build()))
                                .build();
        }

        public Map<String, Object> construirMetadataMinuta(String resumo, Integer idPessoa) {
                Map<String, Object> metadata = new java.util.LinkedHashMap<>();
                metadata.put("idTipoDocumento", 163L);
                metadata.put("numeroAutomaticoDocumento", null);
                metadata.put("resumo", resumo);
                metadata.put("assinado", true);
                metadata.put("idAssunto", 100930257L);
                return metadata;
        }

        /**
         * Protocoliza uma nova solicitação no MPVirtual via SEIU/Protocolo.
         * 
         * @param titulo       Título do processo.
         * @param resumo       Resumo do processo.
         * @param idOrgao      ID do órgão que está protocolando.
         * @param idPessoa     ID da pessoa física interessada.
         * @param documentoPdf Conteúdo do PDF gerado.
         * @return Resultado contendo a resposta e os metadados enviados.
         */
        public ProtocoloResult abrirProcessoCurso(
                        String titulo,
                        String resumo,
                        Long idOrgao,
                        Integer idPessoa,
                        byte[] documentoPdf) {

                log.info("Iniciando protocolização no MPVirtual via OkHttp: {} para o órgão {}", titulo, idOrgao);

                try {
                        // 1. Montar metadados conforme especificado pelo usuário
                        MPVirtualMetadataDto metadata = construirMetadata(resumo, idPessoa);

                        String jsonPayload = objectMapper.writeValueAsString(metadata);
                        log.info("Payload (Metadata) enviado para o MPVirtual: {}", jsonPayload);

                        OkHttpClient client = new OkHttpClient().newBuilder()
                                        .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                        .proxy(java.net.Proxy.NO_PROXY)
                                        .build();
                        // 2. Montar multipart BODY conforme teste de sucesso do usuário
                        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                                        .addFormDataPart("metadata", null,
                                                        RequestBody.create(jsonPayload.getBytes(),
                                                                        MediaType.parse("application/json")))
                                        .addFormDataPart("file", "solicitacao_curso.pdf",
                                                        RequestBody.create(documentoPdf,
                                                                        MediaType.parse("application/octet-stream")))
                                        .build();

                        // 3. Definir token e Cookie conforme o teste de sucesso
                        String token = "Bearer eduardo.ribas";
                        String cookie = "JSESSIONID=F0D60392DBCDB31FEBFBBA4003430A8B";

                        // 4. Construir requisição
                        Request request = new Request.Builder()
                                        .url(mpvirtualUrl + "/api/protocolo")
                                        .post(body)
                                        .addHeader("Authorization", token)
                                        .addHeader("X-Orgao-Id", String.valueOf(idOrgao))
                                        .addHeader("Cookie", cookie)
                                        .build();

                        // 5. Executar requisição
                        Response response = client.newCall(request).execute();

                        String responseStr = response.body() != null ? response.body().string() : "";
                        log.info("Raw response from MPVirtual API: {}", responseStr);

                        if (!response.isSuccessful()) {
                                log.error("Erro no MPVirtual via OkHttp. HTTP {} - Body: {}", response.code(),
                                                responseStr);
                                throw new RuntimeException("HTTP " + response.code() + ": " + responseStr);
                        }

                        MPVirtualProcessoResponseDto processoResponse = objectMapper.readValue(responseStr,
                                        MPVirtualProcessoResponseDto.class);

                        log.info("Protocolo realizado no MPVirtual com sucesso. Processo: {}",
                                        processoResponse.getNumeroProcesso());
                        return new ProtocoloResult(processoResponse, metadata);

                } catch (Exception e) {
                        log.error("Erro ao integrar com MPVirtual via OkHttp: {}", e.getMessage(), e);
                        throw new AppException(e, "Erro na integração com MPVirtual via OkHttp: " + e.getMessage());
                }
        }

        public MPVirtualCriarMinutaResponseDto criarMinuta(
                        String numeroProcesso,
                        String resumo,
                        Long idOrgao,
                        Integer idPessoa,
                        byte[] documentoPdf) {

                log.info("Iniciando criação de minuta no MPVirtual para o processo {}: órgão {}", numeroProcesso,
                                idOrgao);

                try {
                        Object metadata = construirMetadataMinuta(resumo, idPessoa);
                        String jsonPayload = objectMapper.writeValueAsString(metadata);
                        log.info("Chamando criarMinuta no MPVirtual. URL: {}/api/protocolo/{}/minutas | Payload: {}", mpvirtualUrl, numeroProcesso, jsonPayload);

                        OkHttpClient client = new OkHttpClient().newBuilder()
                                        .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                        .proxy(java.net.Proxy.NO_PROXY)
                                        .build();

                        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                                        .addFormDataPart("metadata", null,
                                                        RequestBody.create(jsonPayload.getBytes(),
                                                                        MediaType.parse("application/json")))
                                        .addFormDataPart("file", "retificacao.pdf",
                                                        RequestBody.create(documentoPdf,
                                                                        MediaType.parse("application/pdf")))
                                        .build();

                        String token = "Bearer eduardo.ribas"; // TODO: Unificar gestão de tokens de serviço

                        Request request = new Request.Builder()
                                        .url(mpvirtualUrl + "/api/protocolo/" + numeroProcesso + "/minutas")
                                        .post(body)
                                        .addHeader("Authorization", token)
                                        .addHeader("X-Orgao-Id", String.valueOf(idOrgao))
                                        .build();

                        Response response = client.newCall(request).execute();
                        String responseStr = response.body() != null ? response.body().string() : "";

                        if (!response.isSuccessful()) {
                                log.error("Erro ao criar minuta no MPVirtual. HTTP {} - Body: {}", response.code(),
                                                responseStr);
                                throw new RuntimeException("Erro ao criar minuta: HTTP " + response.code());
                        }

                        return objectMapper.readValue(responseStr, MPVirtualCriarMinutaResponseDto.class);

                } catch (Exception e) {
                        log.error("Erro ao criar minuta no MPVirtual: {}", e.getMessage(), e);
                        throw new AppException(e, "Erro na criação de minuta: " + e.getMessage());
                }
        }

        public void movimentarMinuta(
                        Long idProcesso,
                        Long idMinuta,
                        Long idMovimento,
                        Long idOrgao) {

                log.info("Iniciando movimentação da minuta {} no processo {} com movimento {}", idMinuta, idProcesso, idMovimento);

                try {
                        MPVirtualMovimentarRequestDto payload = MPVirtualMovimentarRequestDto.builder()
                                        .idMinuta(idMinuta)
                                        .idMovimento(idMovimento)
                                        .build();

                        String jsonPayload = objectMapper.writeValueAsString(payload);
                        String url = mpvirtualUrl + "/api/protocolo/" + idProcesso + "/movimentacoes";
                        log.info("Chamando movimentarMinuta no MPVirtual. URL: {} | Payload: {}", url, jsonPayload);

                        OkHttpClient client = new OkHttpClient().newBuilder()
                                        .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                                        .proxy(java.net.Proxy.NO_PROXY)
                                        .build();

                        RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json"));

                        String token = "Bearer eduardo.ribas";

                        Request request = new Request.Builder()
                                        .url(url)
                                        .post(body)
                                        .addHeader("Authorization", token)
                                        .addHeader("X-Orgao-Id", String.valueOf(idOrgao))
                                        .build();

                        Response response = client.newCall(request).execute();
                        if (!response.isSuccessful()) {
                                String responseStr = response.body() != null ? response.body().string() : "";
                                log.error("Erro ao movimentar minuta no MPVirtual. HTTP {} - Body: {}", response.code(),
                                                responseStr);
                                throw new RuntimeException("Erro ao movimentar minuta: HTTP " + response.code());
                        }

                } catch (Exception e) {
                        log.error("Erro ao movimentar minuta no MPVirtual: {}", e.getMessage(), e);
                        throw new AppException(e, "Erro na movimentação de minuta: " + e.getMessage());
                }
        }

        public record ProtocoloResult(MPVirtualProcessoResponseDto response, MPVirtualMetadataDto metadata) {
        }
}

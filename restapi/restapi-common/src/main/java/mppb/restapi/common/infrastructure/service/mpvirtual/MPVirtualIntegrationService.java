package mppb.restapi.common.infrastructure.service.mpvirtual;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.core.util.ByteArrayMultipartFile;
import mppb.restapi.common.infrastructure.client.mpvirtual.MPVirtualClient;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualMetadataDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoResponseDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class MPVirtualIntegrationService {

    private final MPVirtualClient mpVirtualClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public MPVirtualMetadataDto construirMetadata(String resumo, Integer idPessoa) {
        return MPVirtualMetadataDto.builder()
                .idTipoDocumentoPecaInicial(132L) // Fixo: Solicitação de Curso
                .codClasse(910020L)               // Fixo
                .codAssunto(930257L)              // Fixo
                .resumo(resumo)
                .instancia("A")
                .interessados(Collections.singletonList(
                        MPVirtualMetadataDto.InteressadoDto.builder()
                                .idTipoInteressado(72) // Fixo: Interessado
                                .idPessoa(idPessoa)
                                .sigiloso(false)
                                .build()
                ))
                .build();
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

        log.info("Iniciando protocolização no MPVirtual: {} para o órgão {}", titulo, idOrgao);

        try {
            // 1. Montar metadados conforme especificado pelo usuário
            MPVirtualMetadataDto metadata = construirMetadata(resumo, idPessoa);

            // Log do Payload para inspeção no console a pedido do usuário
            try {
                String jsonPayload = objectMapper.writeValueAsString(metadata);
                log.info("Payload (Metadata) enviado para o MPVirtual: {}", jsonPayload);
            } catch (Exception e) {
                log.warn("Não foi possível serializar o payload para log: {}", e.getMessage());
            }

            // 2. Preparar arquivo para Multipart
            ByteArrayMultipartFile file = new ByteArrayMultipartFile(
                    documentoPdf, 
                    "file", 
                    "solicitacao_curso.pdf", 
                    "application/pdf"
            );

            // 3. Definir token (No mundo real viria do SecurityContext ou config)
            // O usuário solicitou a mudança para silvana
            String token = "Bearer scantalice";

            // 4. Serializar o metadata para JSON byte[] e envolver em HttpEntity para garantir o Content-Type: application/json
            byte[] metadataBytes = objectMapper.writeValueAsBytes(metadata);
            HttpHeaders metadataHeaders = new HttpHeaders();
            metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<byte[]> metadataPart = new HttpEntity<>(metadataBytes, metadataHeaders);

            // 5. Chamar a API real
            MPVirtualProcessoResponseDto response = mpVirtualClient.protocolar(token, idOrgao, metadataPart, file);

            log.info("Protocolo realizado no MPVirtual com sucesso. Processo: {}", response.getNumeroProcesso());
            return new ProtocoloResult(response, metadata);

        } catch (Exception e) {
            log.error("Erro ao integrar com MPVirtual: {}", e.getMessage(), e);
            throw new AppException(e, "Erro na integração com MPVirtual: " + e.getMessage());
        }
    }

    public record ProtocoloResult(MPVirtualProcessoResponseDto response, MPVirtualMetadataDto metadata) {}
}

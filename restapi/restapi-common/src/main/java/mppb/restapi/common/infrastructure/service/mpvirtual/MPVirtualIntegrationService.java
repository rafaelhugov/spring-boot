package mppb.restapi.common.infrastructure.service.mpvirtual;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.infrastructure.client.mpvirtual.MPVirtualClient;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoRequestDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoResponseDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MPVirtualIntegrationService {

    private final MPVirtualClient mpVirtualClient;

    /**
     * Initializes a new process in MPVirtual with the generated PDF.
     * 
     * @param titulo The title of the process (e.g. "Solicitação de Curso: [Name]").
     * @param descricao Description of the process.
     * @param idInteressado ID of the user creating the request.
     * @param documentoPdf The generated course request PDF byte array.
     * @return The response containing the process number and ID.
     */
    public MPVirtualProcessoResponseDto abrirProcessoCurso(
            String titulo,
            String descricao,
            Long idInteressado,
            byte[] documentoPdf) {

        log.info("Iniciando abertura de processo no MPVirtual para: {}", titulo);

        try {
            MPVirtualProcessoRequestDto request = MPVirtualProcessoRequestDto.builder()
                    .titulo(titulo)
                    .descricao(descricao)
                    .codigoAssunto("CURSO_EVENTO") // Placeholder subject code
                    .idInteressado(idInteressado)
                    .documentoPdf(documentoPdf)
                    .nomeArquivo("solicitacao_curso.pdf")
                    .dataSolicitacao(LocalDateTime.now())
                    .build();

            // Note: In a real scenario, you would fetch a system token or forward the user's token
            String mockToken = "Bearer MOCK_TOKEN_FOR_MPVIRTUAL";
            
            // Call the external API (can be mocked or configured via application properties)
            // UNCOMMENT NEXT LINE TO ENABLE REAL FEIGN CALL:
            // MPVirtualProcessoResponseDto response = mpVirtualClient.abrirProcessoCurso(mockToken, request);

            // MOCKED RESPONSE FOR NOW
            MPVirtualProcessoResponseDto response = new MPVirtualProcessoResponseDto();
            response.setIdProcesso(12345L);
            response.setNumeroProcesso("PGA-2026-0001MOCK");
            response.setStatus("CRIADO");
            response.setMensagem("Processo criado com sucesso (MOCK).");

            log.info("Processo criado no MPVirtual com número: {}", response.getNumeroProcesso());
            return response;

        } catch (Exception e) {
            log.error("Erro ao integrar com MPVirtual: {}", e.getMessage(), e);
            throw new AppException(e, "Erro na integração com MPVirtual: " + e.getMessage());
        }
    }
}

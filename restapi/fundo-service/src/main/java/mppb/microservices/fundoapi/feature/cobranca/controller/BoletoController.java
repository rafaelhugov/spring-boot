package mppb.microservices.fundoapi.feature.cobranca.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.microservices.common.core.error.NotFoundException;
import mppb.microservices.common.infrastructure.report.JasperService;
import mppb.microservices.common.core.controller.AbstractController;
import mppb.microservices.common.core.util.Constants;
import mppb.microservices.fundoapi.feature.cobranca.converter.BoletoConverter;
import mppb.microservices.fundoapi.feature.cobranca.entity.Boleto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Solicitacao;
import mppb.microservices.fundoapi.feature.cobranca.service.BoletoService;
import mppb.microservices.fundoapi.feature.cobranca.service.SolicitacaoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/boletos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Boletos", description = "Gerencia as operações relacionadas a boletos")
public class BoletoController extends AbstractController {

    private final BoletoService boletoService;

    private final SolicitacaoService solicitacaoService;

    private final BoletoConverter boletoConverter;

    private final JasperService jasperService;

    @Operation(
            summary = "Gerar PDF de boleto",
            description = "Gera um arquivo PDF de um boleto com base no identificador UUID informado. Caso o boleto ainda não tenha linha digitável, ela será gerado no Banco do Brasil antes da criação do PDF.",
            parameters = {
                    @Parameter(
                            name = "uuid",
                            description = "Identificador único do boleto",
                            required = true,
                            example = "123e4567-e89b-12d3-a456-426614174000"
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
                    @ApiResponse(responseCode = "404", description = "Boleto não encontrado", content = @Content)
            }
    )
    @PostMapping(value="/uuid/{uuid}/impressao", produces =  MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> gerarBoleto(@PathVariable UUID uuid) {
        Boleto boleto = boletoService.findByIdentificador(uuid).orElseThrow(() -> new NotFoundException("Boleto " + uuid + " não encontrado"));

        byte[] pdf = boletoService.gerarBoleto(boleto);
        return gerarRespostaPdf(pdf, "boleto.pdf");
    }

    @PostMapping(value="/solicitacao/{idSolicitacao}", produces =  MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> gerarBoletoSolicitacaoCompleta(@PathVariable Long idSolicitacao) {
        Solicitacao solicitacao = solicitacaoService.findById(idSolicitacao).orElseThrow(() -> new NotFoundException("Solicitação " + idSolicitacao + " não encontrada"));
        byte[] pdf = boletoService.gerarBoletos(solicitacao);
        return gerarRespostaPdf(pdf, "boleto.pdf");
    }

}

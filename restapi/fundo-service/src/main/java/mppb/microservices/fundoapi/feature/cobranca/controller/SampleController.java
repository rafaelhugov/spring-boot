package mppb.microservices.fundoapi.feature.cobranca.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.microservices.common.error.BusinessRuleException;
import mppb.microservices.common.infrastructure.report.JasperService;
import mppb.microservices.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.microservices.common.controller.AbstractController;
import mppb.microservices.common.util.Constants;
import mppb.microservices.fundoapi.feature.client.bb.api.BbCobrancaFedcService;
import mppb.microservices.fundoapi.feature.cobranca.converter.AreaConverter;
import mppb.microservices.fundoapi.feature.cobranca.dto.AreaDto;
import mppb.microservices.fundoapi.feature.cobranca.dto.ValorCorrigidoDto;
import mppb.microservices.fundoapi.feature.cobranca.dto.request.AreaSearchRequestDto;
import mppb.microservices.fundoapi.feature.cobranca.dto.response.AutenticacaoDto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Area;
import mppb.microservices.fundoapi.feature.cobranca.projection.AreaSelectProjection;
import mppb.microservices.fundoapi.feature.cobranca.service.AreaService;
import mppb.microservices.fundoapi.feature.cobranca.service.RefisService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/sample")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Exemplos de endpoints", description = "Exemplos de endpoints")
public class SampleController extends AbstractController {

    private final AreaService areaService;

    private final AreaConverter areaConverter;

    private final JasperService jasperService;

    private final RefisService refisService;

    private final BbCobrancaFedcService bbCobrancaService;


    @GetMapping("/corrigir")
    public ValorCorrigidoDto corrigirValor(
            @RequestParam Double valorInicial,
            @RequestParam LocalDate dataInicial,
            @RequestParam LocalDate dataFinal) {

        return refisService.corrigirValor(valorInicial, dataInicial, dataFinal);
    }


    @PatchMapping("/alterarVencimentoBoleto")
    public ResponseEntity<?> alterarVencimento(@RequestParam String nossoNumero,
                                               @RequestParam Long numeroConvenio,
                                               @RequestParam LocalDate novaDataVencimento) {
        try {
            var resposta = bbCobrancaService.alterarDataVencimento(nossoNumero, numeroConvenio, novaDataVencimento);

            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Erro ao alterar vencimento do boleto: " + e.getMessage());
        }
    }

    @PatchMapping("/alterarValorNominal")
    public ResponseEntity<?> alterarValorNominal(@RequestParam String nossoNumero,
                                                @RequestParam Long numeroConvenio,
                                                @RequestParam Double novoValorNominal) {
        try {
            var resposta = bbCobrancaService.alterarValorNominal(nossoNumero, numeroConvenio, novoValorNominal);
            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Erro ao alterar valor nominal do boleto: " + e.getMessage());
        }
    }

    @Operation(summary = "Obtém áreas mas dá erro", description = "Vai dar exceção.")
    @GetMapping(value="/exception", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<AreaDto>> vaiDarExcecao() {
        throw new BusinessRuleException("teste000", new String[]{"(valor de teste)"});
    }

    @Operation(summary = "Obtém áreas", description = "Retorna uma lista áreas.")
    @GetMapping(value="", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<AreaDto>> buscaAreas(@Valid @ModelAttribute AreaSearchRequestDto areaSearchDto, Pageable pageable) {
        Page<Area> areas = Optional.ofNullable(areaSearchDto.getTermo())
                .filter(termo -> !termo.isBlank())
                .map(termo -> areaService.findByNomeContainingIgnoreCase(termo, pageable))
                .orElseGet(() -> areaService.findAll(pageable));

        return ResponseEntity.ok(areas.map(areaConverter::toDto));
    }

    @Operation(summary = "Busca o response do usuário autenticado", description = "Retorna info sobre tokens.")
    @GetMapping(value="/response", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AutenticacaoDto> tokenAutenticacao(@Parameter(hidden = true) TokenAuthentication auth) {
        if(auth == null){
            return ResponseEntity.notFound().build();
        }
        String token = auth.getToken();
        Long usuarioId = auth.getUsuario().getId();
        String usuarioNome = auth.getUsuario().getNome();
        String login = auth.getUsuario().getLogin();

        Map.of("token", auth.getToken(), "usuario", Map.of("nome", usuarioNome, "id", usuarioId, "login" , login));

        return ResponseEntity.ok(AutenticacaoDto.builder().token(token).usuarioId(usuarioId).usuarioLogin(login).usuarioNome(usuarioNome).build());
    }

    @Operation(summary = "Jsom de um mapa java", description = "Retorna json de um mapa.")
    @GetMapping(value="/de-mapa", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jsonFromMap() {
        return ResponseEntity.ok(Map.of("campo1", "valor1", "campocomposto1", Map.of("subcampo1", "subvalor1", "subcampo2", "subvalor2")));
    }

    @Operation(summary = "Obtém áreas usando argumento termo", description = "Retorna uma lista áreas.")
    @GetMapping(value="/areas/termo-simples", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<AreaDto>> buscaAreas(@RequestParam(value = "termo", required = true)  String termo, Pageable pageable) {
        System.out.println("termo:"+termo);
        Page<Area> areas = areaService.findByNomeContainingIgnoreCase(termo, pageable);
        return ResponseEntity.ok(areas.map(areaConverter::toDto));
    }

    @Operation(summary = "Obtém áreas para montar comboboxes utilizando projeções Jpa", description = "Retorna uma lista áreas, id e descrição.")
    @GetMapping(value="/areas/select/projection", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AreaDto>> areaSelectProjection() {
        List<AreaSelectProjection> areas = areaService.findSelectAll();
        List<AreaDto> areaDto = areas.stream().map(a -> new AreaDto(a.getId(), a.getNome())).collect(Collectors.toList());
        return ResponseEntity.ok(areaDto);
    }


    @Operation(summary = "Obtém áreas para montar comboboxes utilizando createNativeQuery", description = "Retorna uma lista áreas, id e descrição.")
    @GetMapping(value="/areas/select/native", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AreaDto>> areaSelectNative() {
        List<AreaDto> areaDto = areaService.findSelectNative();
        return ResponseEntity.ok(areaDto);
    }

    @Operation(summary = "Obtém áreas para montar comboboxes utilizando JPQL", description = "Retorna uma lista áreas, id e descrição.")
    @GetMapping(value="/areas/select/jpql", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AreaDto>> areaSelectJpql() {
        List<AreaDto> areaDto = areaService.findSelectJpql();
        return ResponseEntity.ok(areaDto);
    }

    @GetMapping(value="/areas/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AreaDto> getById(@PathVariable Long id) {
        return areaService.findById(id)
                .map(areaConverter::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AreaDto> create(@RequestBody AreaDto areaDto) {
        Area createdArea = areaService.save(areaConverter.toEntity(areaDto));
        return ResponseEntity.ok(areaConverter.toDto(createdArea));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaDto> update(@PathVariable Long id, @RequestBody AreaDto areaDto) {
        return areaService.findById(id)
                .map(existing -> {
                    areaDto.setId(id); // Garante que o ID do path é o mesmo do objeto
                    Area updatedArea = areaService.save(areaConverter.toEntity(areaDto));
                    return ResponseEntity.ok(areaConverter.toDto(updatedArea));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/areas/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        return areaService.findById(id)
                .map(area -> {
                    areaService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/relatorio", produces =  MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Relatório PDF de teste sem conexão com banco de dados",
            description = "Gera um relatório no formato PDF vazio (sem dados) com base no template JasperReports."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relatório PDF gerado com sucesso",
                    content = @Content(mediaType =  MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "500", description = "Erro interno ao gerar o relatório")
    })
    public ResponseEntity<byte[]> gerarRelatorioPdf() {
        Map<String, Object> parametros = new HashMap<>();
        byte[] pdf = jasperService.gerarPdfEmptyDatasource("report.jasper", parametros);
        return gerarRespostaPdf(pdf, "relatorio.pdf");
    }

    @GetMapping(value = "/relatorio2", produces =  MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Relatório PDF de teste com conexão com banco de dados",
            description = "Gera um relatório em PDF com a palavra TESTE e com a hora atual recuperada através de conexão com banco de dados"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relatório PDF gerado com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "500", description = "Erro interno ao gerar o relatório")
    })
    public ResponseEntity<byte[]> gerarRelatorioComConsultaAoBancoDeDados() {
        Map<String, Object> parametros = new HashMap<>();
        byte[] pdf = jasperService.gerarPdfConnection("report2.jasper", parametros);
        return gerarRespostaPdf(pdf, "relatorio.pdf");
    }

}

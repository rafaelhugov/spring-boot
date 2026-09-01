package mppb.restapi.mobile.feature.processoconsulta.controller;

import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoFilterDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoResponseDTO;
import mppb.restapi.mobile.feature.processoconsulta.service.ProcessoConsultaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/v1/processos")
public class ProcessoConsultaPublicaController {

    @Autowired
    private ProcessoConsultaService consultaService;

    @GetMapping("/filtros")
    public ResponseEntity<?> listarFiltros() {
        return ResponseEntity.ok(consultaService.listarFiltros());
    }

    @PostMapping("/consulta")
    public ResponseEntity<?> consultarProcessos(@RequestBody ConsultaProcessoFilterDTO filtro) {
        try {
            List<ConsultaProcessoResponseDTO> processos = consultaService.consultarProcessos(filtro);
            return ResponseEntity.ok(processos);
        } catch (IllegalArgumentException e) {
            // Retorna 400 Bad Request caso a validação falhe (Filtro mínimo ou CAPTCHA inválido)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Em caso de erro interno, evitamos expor stacktrace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ocorreu um erro interno ao processar a consulta. Tente novamente mais tarde.");
        }
    }
}

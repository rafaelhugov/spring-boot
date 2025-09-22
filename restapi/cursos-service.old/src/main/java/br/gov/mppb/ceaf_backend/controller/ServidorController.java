package br.gov.mppb.ceaf_backend.controller;

import br.gov.mppb.ceaf_backend.service.ServidorService;
import br.gov.mppb.ceaf_backend.service.dto.ServidorDTO;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/servidores")
public class ServidorController {

    private final ServidorService servidorService;

    public ServidorController(ServidorService servidorService) {
        this.servidorService = servidorService;
    }

    @GetMapping("/buscar")
    public List<ServidorDTO> buscarServidores(@RequestParam String nome) {
        return servidorService.buscarServidoresPorNome(nome);
    }
}
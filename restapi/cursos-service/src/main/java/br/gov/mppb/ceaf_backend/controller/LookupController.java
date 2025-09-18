package br.gov.mppb.ceaf_backend.controller;

import br.gov.mppb.ceaf_backend.domain.*;
import br.gov.mppb.ceaf_backend.repository.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LookupController {

    private final ModalidadeRepository modalidadeRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final PublicoAlvoRepository publicoAlvoRepository;
   // private final EntidadeTutoraRepository entidadeTutoraRepository;
    private final PapelMinistranteRepository papelMinistranteRepository;// Adicionar

    public LookupController(ModalidadeRepository modalidadeRepository, TipoEventoRepository tipoEventoRepository, PublicoAlvoRepository publicoAlvoRepository, PapelMinistranteRepository papelMinistranteRepository) { // Atualizar construtor
        this.modalidadeRepository = modalidadeRepository;
        this.tipoEventoRepository = tipoEventoRepository;
        this.publicoAlvoRepository = publicoAlvoRepository;
        //this.entidadeTutoraRepository = entidadeTutoraRepository;
        this.papelMinistranteRepository = papelMinistranteRepository;
    }

    @GetMapping("/modalidades")
    public List<Modalidade> getModalidades() {
        return modalidadeRepository.findAll();
    }

    @GetMapping("/tipos-evento")
    public List<TipoEvento> getTiposEvento() {
        return tipoEventoRepository.findAll();
    }

    @GetMapping("/publico-alvo")
    public List<PublicoAlvo> getPublicoAlvo() {
        return publicoAlvoRepository.findAll();
    }

//    @GetMapping("/entidades-tutoras")
//    public List<EntidadeTutora> getEntidadesTutoras() { return entidadeTutoraRepository.findAll(); }

    @GetMapping("/papeis-ministrantes")
    public List<PapelMinistrante> getPapeisMinistrantes() {
        return papelMinistranteRepository.findAll();
    }
}

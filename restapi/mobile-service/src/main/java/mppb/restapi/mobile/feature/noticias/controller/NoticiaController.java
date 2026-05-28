package mppb.restapi.cursos.feature.noticias.controller;

import mppb.restapi.cursos.feature.noticias.dto.NoticiaDto;
import mppb.restapi.cursos.feature.noticias.service.NoticiaService;
import mppb.restapi.common.infrastructure.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/" + Constants.API_PREFIX + "/public/noticias")
public class NoticiaController {

    @Autowired
    private NoticiaService noticiaService;

    @GetMapping
    public List<NoticiaDto> getNoticias() {
        return noticiaService.getUltimasNoticias();
    }
}

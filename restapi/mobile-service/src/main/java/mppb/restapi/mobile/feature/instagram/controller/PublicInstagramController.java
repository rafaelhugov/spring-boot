package mppb.restapi.mobile.feature.instagram.controller;

import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.feature.instagram.dto.InstagramPostDto;
import mppb.restapi.mobile.feature.instagram.service.InstagramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/" + Constants.API_PREFIX + "/public/instagram")
public class PublicInstagramController {

    @Autowired
    private InstagramService instagramService;

    @GetMapping
    public ResponseEntity<List<InstagramPostDto>> getPosts(
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(instagramService.getPosts(limit));
    }
}


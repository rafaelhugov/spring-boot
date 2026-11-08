package mppb.restapi.mobile.feature.appversion.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.feature.appversion.config.AppUpdateProperties;
import mppb.restapi.mobile.feature.appversion.dto.AppVersaoResponseDTO;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/" + Constants.API_PREFIX + "/public/app-versao")
public class AppVersaoController {

    private final AppUpdateProperties properties;

    @Autowired
    public AppVersaoController(AppUpdateProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ResponseEntity<AppVersaoResponseDTO> verificarVersao(
            @RequestParam(value = "build", required = false, defaultValue = "0") int build,
            @RequestParam(value = "platform", required = false, defaultValue = "android") String platform,
            @RequestHeader(value = "X-App-Build", required = false) Integer headerBuild,
            @RequestHeader(value = "X-App-Platform", required = false) String headerPlatform) {

        int effectiveBuild = (headerBuild != null && headerBuild > 0) ? headerBuild : build;
        String effectivePlatform = (headerPlatform != null && !headerPlatform.isBlank())
                ? headerPlatform.trim().toLowerCase()
                : platform.trim().toLowerCase();

        int minBuild = properties.getMinBuildParaPlataforma(effectivePlatform);
        boolean forceUpdate = minBuild > 0 && effectiveBuild < minBuild;

        AppVersaoResponseDTO response = AppVersaoResponseDTO.builder()
                .forceUpdate(forceUpdate)
                .minBuild(minBuild)
                .currentBuild(effectiveBuild)
                .platform(effectivePlatform)
                .storeUrl(properties.getStoreUrlParaPlataforma(effectivePlatform))
                .message(properties.getMensagem())
                .build();

        return ResponseEntity.ok(response);
    }
}


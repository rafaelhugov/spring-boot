package mppb.restapi.mobile.feature.appversion.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import mppb.restapi.mobile.feature.appversion.config.AppUpdateProperties;
import mppb.restapi.mobile.feature.appversion.dto.AppVersaoResponseDTO;

class AppVersaoControllerTest {

    private AppUpdateProperties properties;
    private AppVersaoController controller;

    @BeforeEach
    void setUp() {
        properties = new AppUpdateProperties();
        properties.setMinBuildAndroid(129);
        properties.setMinBuildIos(130);
        properties.setStoreUrlAndroid("https://play.google.com/store/apps/details?id=br.mp.mppbapp.app");
        properties.setStoreUrlIos("https://apps.apple.com/app/mppb/id6825377891");
        properties.setMensagem("Atualização necessária.");
        controller = new AppVersaoController(properties);
    }

    @Test
    void deveExigirAtualizacaoQuandoBuildMenorQueMinimoAndroid() {
        ResponseEntity<AppVersaoResponseDTO> response = controller.verificarVersao(
                128, "android", null, null);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isForceUpdate());
        assertEquals(129, response.getBody().getMinBuild());
        assertEquals(128, response.getBody().getCurrentBuild());
        assertEquals("android", response.getBody().getPlatform());
        assertEquals("https://play.google.com/store/apps/details?id=br.mp.mppbapp.app", response.getBody().getStoreUrl());
    }

    @Test
    void naoDeveExigirAtualizacaoQuandoBuildIgualOuMaiorQueMinimoAndroid() {
        ResponseEntity<AppVersaoResponseDTO> response = controller.verificarVersao(
                129, "android", null, null);

        assertNotNull(response.getBody());
        assertFalse(response.getBody().isForceUpdate());
        assertEquals(129, response.getBody().getMinBuild());
        assertEquals(129, response.getBody().getCurrentBuild());
    }

    @Test
    void deveRespeitarHeadersQuandoEnviados() {
        ResponseEntity<AppVersaoResponseDTO> response = controller.verificarVersao(
                0, "android", 125, "ios");

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isForceUpdate());
        assertEquals(130, response.getBody().getMinBuild());
        assertEquals(125, response.getBody().getCurrentBuild());
        assertEquals("ios", response.getBody().getPlatform());
        assertEquals("https://apps.apple.com/app/mppb/id6825377891", response.getBody().getStoreUrl());
    }

    @Test
    void naoDeveExigirAtualizacaoQuandoMinBuildForZero() {
        properties.setMinBuildAndroid(0);
        ResponseEntity<AppVersaoResponseDTO> response = controller.verificarVersao(
                100, "android", null, null);

        assertNotNull(response.getBody());
        assertFalse(response.getBody().isForceUpdate());
    }
}

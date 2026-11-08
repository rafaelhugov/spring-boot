package mppb.restapi.mobile.feature.appversion.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import mppb.restapi.mobile.feature.appversion.config.AppUpdateProperties;
import mppb.restapi.mobile.feature.appversion.dto.AppVersaoResponseDTO;

class AppVersionInterceptorTest {

    private AppUpdateProperties properties;
    private ObjectMapper objectMapper;
    private AppVersionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new AppUpdateProperties();
        properties.setMinBuildAndroid(129);
        properties.setMinBuildIos(130);
        properties.setStoreUrlAndroid("https://play.google.com/store/apps/details?id=br.mp.mppbapp.app");
        properties.setStoreUrlIos("https://apps.apple.com/app/mppb/id6825377891");
        properties.setMensagem("Atualize o app.");
        objectMapper = new ObjectMapper();
        interceptor = new AppVersionInterceptor(properties, objectMapper);
    }

    @Test
    void deveBloquearComStatus426QuandoBuildForMenorQueMinimo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/contracheques");
        request.addHeader("X-App-Build", "128");
        request.addHeader("X-App-Platform", "android");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertFalse(proceed);
        assertEquals(426, response.getStatus());

        AppVersaoResponseDTO dto = objectMapper.readValue(response.getContentAsString(), AppVersaoResponseDTO.class);
        assertTrue(dto.isForceUpdate());
        assertEquals(129, dto.getMinBuild());
        assertEquals(128, dto.getCurrentBuild());
        assertEquals("https://play.google.com/store/apps/details?id=br.mp.mppbapp.app", dto.getStoreUrl());
    }

    @Test
    void devePermitirQuandoBuildForIgualOuSuperior() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/contracheques");
        request.addHeader("X-App-Build", "129");
        request.addHeader("X-App-Platform", "android");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertTrue(proceed);
        assertEquals(200, response.getStatus());
    }

    @Test
    void devePermitirRotasExcluidasMesmoComBuildAntiga() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/health");
        request.addHeader("X-App-Build", "100");
        request.addHeader("X-App-Platform", "android");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertTrue(proceed);
    }

    @Test
    void devePermitirRequisicoesSemHeaderBuild() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/contracheques");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertTrue(proceed);
    }
}


package mppb.restapi.mobile.feature.appversion.interceptor;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import mppb.restapi.mobile.feature.appversion.config.AppUpdateProperties;
import mppb.restapi.mobile.feature.appversion.dto.AppVersaoResponseDTO;

@Component
public class AppVersionInterceptor implements HandlerInterceptor {

    private final AppUpdateProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    public AppVersionInterceptor(AppUpdateProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (isPathExcluded(path)) {
            return true;
        }

        String buildHeader = request.getHeader("X-App-Build");
        String platformHeader = request.getHeader("X-App-Platform");

        if (buildHeader != null && !buildHeader.isBlank()) {
            try {
                int clientBuild = Integer.parseInt(buildHeader.trim());
                String platform = (platformHeader != null && !platformHeader.isBlank())
                        ? platformHeader.trim().toLowerCase()
                        : "android";

                int minBuild = properties.getMinBuildParaPlataforma(platform);

                if (minBuild > 0 && clientBuild < minBuild) {
                    escreverRespostaUpgradeRequired(response, clientBuild, minBuild, platform);
                    return false;
                }
            } catch (NumberFormatException ignored) {
                // Se header inválido, não bloqueia por formato inesperado
            }
        }

        return true;
    }

    private boolean isPathExcluded(String path) {
        if (path == null) {
            return true;
        }
        return path.contains("/public/health")
                || path.contains("/public/app-versao")
                || path.contains("/swagger")
                || path.contains("/v3/api-docs")
                || path.contains("/actuator")
                || path.equals("/error");
    }

    private void escreverRespostaUpgradeRequired(
            HttpServletResponse response,
            int clientBuild,
            int minBuild,
            String platform) throws IOException {

        response.setStatus(426); // HTTP 426 Upgrade Required
        response.setContentType("application/json;charset=UTF-8");

        AppVersaoResponseDTO dto = AppVersaoResponseDTO.builder()
                .forceUpdate(true)
                .minBuild(minBuild)
                .currentBuild(clientBuild)
                .platform(platform)
                .storeUrl(properties.getStoreUrlParaPlataforma(platform))
                .message(properties.getMensagem())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(dto));
    }
}

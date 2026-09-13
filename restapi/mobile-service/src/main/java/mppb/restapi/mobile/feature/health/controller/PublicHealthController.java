package mppb.restapi.mobile.feature.health.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mppb.restapi.common.infrastructure.util.Constants;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/" + Constants.API_PREFIX + "/public/health")
public class PublicHealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString());
    }
}

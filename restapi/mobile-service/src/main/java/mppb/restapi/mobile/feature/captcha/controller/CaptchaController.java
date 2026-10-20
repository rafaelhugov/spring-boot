package mppb.restapi.mobile.feature.captcha.controller;

import mppb.restapi.mobile.feature.captcha.dto.CaptchaResponseDTO;
import mppb.restapi.mobile.feature.captcha.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/v1/captcha")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    @GetMapping
    public ResponseEntity<CaptchaResponseDTO> getCaptcha() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(captchaService.generateCaptcha());
    }
}

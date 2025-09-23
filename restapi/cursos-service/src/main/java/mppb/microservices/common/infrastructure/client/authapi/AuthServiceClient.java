package mppb.microservices.common.infrastructure.client.authapi;


import mppb.microservices.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.microservices.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.microservices.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.microservices.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

@FeignClient(name = "auth-service", url = "${api.auth.url:http://localhost:9001}", path = "/acesso")
public interface AuthServiceClient {

    @PostMapping("/autoriza")
    public AutorizaResponseDto autoriza(@Valid @RequestBody AutorizaRequestDto autorizaRequestTO);

    @PostMapping("/consulta-autorizacao")
    public ConsultaAutorizacaoResponseDto consultaAutorizacao(@Valid @RequestBody ConsultaAutorizacaoRequestDto consultaAutorizacaoRequestTO);

}

package mppb.microservices.common.client.authapi;


import mppb.microservices.common.client.authapi.dto.AutorizaRequestTO;
import mppb.microservices.common.client.authapi.dto.AutorizaResponseTO;
import mppb.microservices.common.client.authapi.dto.ConsultaAutorizacaoRequestTO;
import mppb.microservices.common.client.authapi.dto.ConsultaAutorizacaoResponseTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

@FeignClient(name = "auth-service", url = "${auth-service.url:http://localhost:9001}", path = "/acesso")
public interface AuthServiceClient {

    @PostMapping("/autoriza")
    public AutorizaResponseTO autoriza(@Valid @RequestBody AutorizaRequestTO autorizaRequestTO);

    @PostMapping("/consulta-autorizacao")
    public ConsultaAutorizacaoResponseTO consultaAutorizacao(@Valid @RequestBody ConsultaAutorizacaoRequestTO consultaAutorizacaoRequestTO);

}

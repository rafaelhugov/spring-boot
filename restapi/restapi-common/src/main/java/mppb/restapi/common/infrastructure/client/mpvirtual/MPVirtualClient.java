package mppb.restapi.common.infrastructure.client.mpvirtual;

import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoRequestDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "mpvirtual-service", url = "${api.mpvirtual.url:http://localhost:9005}", path = "/api/v1/processos")
public interface MPVirtualClient {

    @PostMapping("/abrir-curso")
    MPVirtualProcessoResponseDto abrirProcessoCurso(
            @RequestHeader("Authorization") String token,
            @RequestBody MPVirtualProcessoRequestDto request
    );
}

package mppb.restapi.common.infrastructure.client.mpvirtual;

import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualMetadataDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "mpvirtual-service", url = "${api.mpvirtual.url:http://localhost:8082}")
public interface MPVirtualClient {

    @PostMapping(value = "/api/protocolo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MPVirtualProcessoResponseDto protocolar(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Orgao-Id") Long idOrgao,
            @RequestPart("metadata") MPVirtualMetadataDto metadata,
            @RequestPart("file") MultipartFile file
    );
}

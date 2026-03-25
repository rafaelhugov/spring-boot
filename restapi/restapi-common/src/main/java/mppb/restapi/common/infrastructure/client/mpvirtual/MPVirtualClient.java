package mppb.restapi.common.infrastructure.client.mpvirtual;

import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualMetadataDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualProcessoResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "mpvirtual-service", url = "${api.mpvirtual.url:http://10.128.4.14:8082}")
public interface MPVirtualClient {

    @PostMapping(value = "/api/protocolo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MPVirtualProcessoResponseDto protocolar(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Cookie", required = false) String cookie,
            @RequestHeader("X-Orgao-Id") Long idOrgao,
            @RequestPart(value = "metadata") HttpEntity<MPVirtualMetadataDto> metadata,
            @RequestPart("file") MultipartFile file
    );

    @PostMapping(value = "/api/protocolo/{numeroProcesso}/minutas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualCriarMinutaResponseDto criarMinuta(
            @org.springframework.web.bind.annotation.PathVariable("numeroProcesso") String numeroProcesso,
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Orgao-Id") Long idOrgao,
            @RequestPart(value = "metadata") HttpEntity<MPVirtualMetadataDto> metadata,
            @RequestPart("file") MultipartFile file
    );

    @PostMapping(value = "/api/protocolo/{idMinuta}/movimentacoes", consumes = MediaType.APPLICATION_JSON_VALUE)
    void movimentarMinuta(
            @org.springframework.web.bind.annotation.PathVariable("idMinuta") Long idMinuta,
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Orgao-Id") Long idOrgao,
            @org.springframework.web.bind.annotation.RequestBody mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualMovimentarRequestDto request
    );
}

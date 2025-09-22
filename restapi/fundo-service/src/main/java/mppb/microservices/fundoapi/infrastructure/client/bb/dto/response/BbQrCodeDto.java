package mppb.microservices.fundoapi.infrastructure.client.bb.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BbQrCodeDto {
    private String url;
    private String txId;
    private String emv;
}

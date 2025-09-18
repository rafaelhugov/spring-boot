package mppb.microservices.fundoapi.feature.client.bb.dto.response;

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

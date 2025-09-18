package mppb.microservices.fundoapi.feature.cobranca.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AutenticacaoDto {

    private String token;
    private String usuarioLogin;
    private String usuarioNome;
    private Long usuarioId;

}

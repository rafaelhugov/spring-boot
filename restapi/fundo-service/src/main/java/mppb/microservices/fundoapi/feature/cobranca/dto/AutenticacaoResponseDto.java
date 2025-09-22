package mppb.microservices.fundoapi.feature.cobranca.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AutenticacaoResponseDto {

    private String token;
    private String usuarioLogin;
    private String usuarioNome;
    private Long usuarioId;

}

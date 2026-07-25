package mppb.restapi.common.infrastructure.client.authapi.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiTokenDtoTest {

    @Test
    void autenticaResponseToStringNaoExpoeToken() {
        AutenticaResponseDto dto = new AutenticaResponseDto("valor-sensivel");

        assertThat(dto.toString())
                .contains("***")
                .doesNotContain("valor-sensivel");
    }

    @Test
    void consultaAutorizacaoRequestToStringNaoExpoeToken() {
        ConsultaAutorizacaoRequestDto dto = new ConsultaAutorizacaoRequestDto("valor-sensivel", "cursos");

        assertThat(dto.toString())
                .contains("***")
                .contains("cursos")
                .doesNotContain("valor-sensivel");
    }
}

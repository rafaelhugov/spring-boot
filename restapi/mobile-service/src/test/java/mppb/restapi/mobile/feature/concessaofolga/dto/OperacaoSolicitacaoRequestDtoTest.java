package mppb.restapi.mobile.feature.concessaofolga.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class OperacaoSolicitacaoRequestDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializaApenasCamposPreenchidos() throws Exception {
        OperacaoSolicitacaoRequestDto request = new OperacaoSolicitacaoRequestDto();
        request.setDataAntecipacao("06/10/2026");

        String json = objectMapper.writeValueAsString(request);

        assertEquals("{\"dataAntecipacao\":\"06/10/2026\"}", json);
    }
}

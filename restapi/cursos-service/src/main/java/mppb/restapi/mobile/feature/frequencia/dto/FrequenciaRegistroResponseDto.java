package mppb.restapi.mobile.feature.frequencia.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de resposta simplificada após um registro de frequência bem-sucedido na interface pública.
 * Contém apenas o essencial para a mensagem de confirmação no frontend.
 */
@Data
@Builder
public class FrequenciaRegistroResponseDto {
    private String nomeParticipante; // Nome completo para extração do primeiro nome no frontend
    private LocalDateTime dataHoraRegistro;
}
package mppb.restapi.mobile.feature.frequencia.dto;

import lombok.Data;
import mppb.restapi.mobile.feature.inscricao.dto.InscricaoDto;

import java.time.LocalDateTime;

/**
 * DTO simplificado para a exibição do registro de frequência, contendo
 * os dados necessários da Inscrição e da Chamada.
 */
@Data
public class FrequenciaDto {
    private Long id;
    private LocalDateTime dataHoraRegistro;
    private InscricaoDto inscricao;
    private ChamadaDto chamada;
}
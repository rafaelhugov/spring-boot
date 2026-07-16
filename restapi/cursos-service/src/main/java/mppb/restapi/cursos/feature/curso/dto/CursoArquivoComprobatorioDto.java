package mppb.restapi.cursos.feature.curso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoArquivoComprobatorioDto {
    private Long id;
    private Long arquivoId;
    private String nomeArquivo;
    private String mimeType;
    private Long tamanho;
    private LocalDateTime dataRegistro;
}

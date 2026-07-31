package mppb.restapi.mobile.feature.designacao.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesignacoesAtivasResponse {
    private String tipoUsuario;
    private List<DesignacaoAtivaItemResponse> designacoes;
}

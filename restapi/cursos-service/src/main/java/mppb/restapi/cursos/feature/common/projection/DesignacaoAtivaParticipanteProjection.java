package mppb.restapi.cursos.feature.common.projection;

import java.time.LocalDate;

public interface DesignacaoAtivaParticipanteProjection {

    Integer getDesignacaoId();

    String getTipoLetra();

    String getTipoDescricao();

    String getFuncaoNome();

    String getOrgaoNome();

    String getOrgaoExecucaoNome();

    String getAtoNumero();

    LocalDate getDataInicial();

    LocalDate getDataFinal();

    LocalDate getDataPublicacao();
}

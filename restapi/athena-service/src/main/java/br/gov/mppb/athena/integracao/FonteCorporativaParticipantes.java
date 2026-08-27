package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.seguranca.DadosParticipanteCorporativo;
import java.util.List;

public interface FonteCorporativaParticipantes {

    List<DadosParticipanteCorporativo> pesquisar(ConsultaParticipantesCorporativos consulta);
}

package br.gov.mppb.athena.dominio;

import java.time.OffsetDateTime;
import java.util.Set;

public record ProjetoInstitucional(
        IdentificadorProjeto identificador,
        String nome,
        String descricao,
        Set<String> unidades,
        Set<ParticipanteProjeto> participantes,
        SituacaoProjeto situacao,
        OffsetDateTime criadoEm,
        UsuarioInstitucional criadoPor
) {

    public ProjetoInstitucional {
        if (identificador == null) {
            throw new IllegalArgumentException("identificador do projeto deve ser informado.");
        }
        nome = TextoObrigatorio.validar(nome, "nome do projeto");
        descricao = TextoObrigatorio.validar(descricao, "descricao do projeto");
        PoliticaDominioMultiprojeto.rejeitarTermoDeProjetoPiloto(nome, "nome do projeto");
        PoliticaDominioMultiprojeto.rejeitarTermoDeProjetoPiloto(descricao, "descricao do projeto");
        unidades = copiarConjuntoObrigatorio(unidades, "unidades do projeto");
        participantes = copiarConjuntoObrigatorio(participantes, "participantes do projeto");
        if (situacao == null) {
            throw new IllegalArgumentException("situacao do projeto deve ser informada.");
        }
        if (criadoEm == null) {
            throw new IllegalArgumentException("data de criacao do projeto deve ser informada.");
        }
        if (criadoPor == null) {
            throw new IllegalArgumentException("usuario criador do projeto deve ser informado.");
        }
    }

    private static <T> Set<T> copiarConjuntoObrigatorio(Set<T> valores, String campo) {
        if (valores == null || valores.isEmpty()) {
            throw new IllegalArgumentException(campo + " devem ser informados.");
        }
        return Set.copyOf(valores);
    }
}

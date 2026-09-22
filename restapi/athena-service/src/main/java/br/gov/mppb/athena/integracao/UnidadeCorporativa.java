package br.gov.mppb.athena.integracao;

public record UnidadeCorporativa(
        Integer idPessoa,
        String nome,
        String sigla
) {
    public String nomeApresentacao() {
        return sigla == null || sigla.isBlank() ? nome : sigla + " - " + nome;
    }
}

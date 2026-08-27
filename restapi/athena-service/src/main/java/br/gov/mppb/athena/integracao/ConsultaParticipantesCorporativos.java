package br.gov.mppb.athena.integracao;

public record ConsultaParticipantesCorporativos(
        String termo,
        String matricula,
        String unidadeOuLotacao
) {
    private static final int TAMANHO_MINIMO_FILTRO = 3;

    public ConsultaParticipantesCorporativos {
        termo = normalizar(termo);
        matricula = normalizar(matricula);
        unidadeOuLotacao = normalizar(unidadeOuLotacao);
        if (termo == null && matricula == null && unidadeOuLotacao == null) {
            throw new IllegalArgumentException("ao menos um filtro corporativo deve ser informado.");
        }
        validarTamanhoMinimo(termo, "termo de pesquisa");
        validarTamanhoMinimo(matricula, "matricula");
        validarTamanhoMinimo(unidadeOuLotacao, "unidade ou lotacao");
    }

    public static ConsultaParticipantesCorporativos porTermo(String termo) {
        return new ConsultaParticipantesCorporativos(
                validarTexto(termo, "termo de pesquisa"),
                null,
                null
        );
    }

    private static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " deve ser informado.");
        }
        return valor.trim();
    }

    private static void validarTamanhoMinimo(String valor, String campo) {
        if (valor != null && valor.length() < TAMANHO_MINIMO_FILTRO) {
            throw new IllegalArgumentException(campo + " deve ter ao menos 3 caracteres.");
        }
    }
}

package mppb.microservices.fundoapi.feature.cobranca.enums;

public enum TipoSolicitacaoEnum {
    FDD("FDD", "Fundo de Direitos Difusos"),
    FEDC("FEDC", "Fundo Especial de Direito do Consumidor");

    private final String codigo;
    private final String label;

    TipoSolicitacaoEnum(String codigo, String label) {
        this.codigo = codigo;
        this.label = label;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getLabel() {
        return label;
    }

    public static TipoSolicitacaoEnum fromCodigo(String codigo) {
        for (TipoSolicitacaoEnum e : values()) {
            if (e.codigo.equalsIgnoreCase(codigo)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Valor inválido: " + codigo);
    }
}

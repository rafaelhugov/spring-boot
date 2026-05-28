package mppb.restapi.cursos.feature.frequencia.enums;

public enum TipoChamadaEnum {
    INICIO("Início da Aula"),
    FIM("Fim da Aula");

    private final String descricao;

    TipoChamadaEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

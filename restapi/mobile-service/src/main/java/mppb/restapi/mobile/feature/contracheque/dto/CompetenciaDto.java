package mppb.restapi.mobile.feature.contracheque.dto;

import java.util.Objects;

public class CompetenciaDto {
    private Integer mes;
    private Integer ano;

    public CompetenciaDto() {
    }

    public CompetenciaDto(Integer mes, Integer ano) {
        this.mes = mes;
        this.ano = ano;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompetenciaDto that = (CompetenciaDto) o;
        return Objects.equals(mes, that.mes) && Objects.equals(ano, that.ano);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mes, ano);
    }
}

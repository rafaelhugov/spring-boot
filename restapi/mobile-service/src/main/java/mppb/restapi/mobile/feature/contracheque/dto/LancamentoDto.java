package mppb.restapi.mobile.feature.contracheque.dto;

import java.math.BigDecimal;

public class LancamentoDto {
    private String codigo;
    private String descricao;
    private BigDecimal valor;
    private String tipo;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}

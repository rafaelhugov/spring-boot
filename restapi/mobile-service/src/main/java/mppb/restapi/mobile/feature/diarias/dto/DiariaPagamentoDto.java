package mppb.restapi.mobile.feature.diarias.dto;

import java.math.BigDecimal;

public class DiariaPagamentoDto {

    private String cdOrgao;
    private String sigla;
    private String nuExercicio;
    private String documento;
    private String dtPagamento;
    private BigDecimal vlBruto;
    private BigDecimal vlLiquido;
    private String dsModalidadePag;
    private String nuProcesso;
    private String cdElemento;
    private String descricaoElemento;

    public String getCdOrgao() { return cdOrgao; }
    public void setCdOrgao(String cdOrgao) { this.cdOrgao = cdOrgao; }

    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }

    public String getNuExercicio() { return nuExercicio; }
    public void setNuExercicio(String nuExercicio) { this.nuExercicio = nuExercicio; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getDtPagamento() { return dtPagamento; }
    public void setDtPagamento(String dtPagamento) { this.dtPagamento = dtPagamento; }

    public BigDecimal getVlBruto() { return vlBruto; }
    public void setVlBruto(BigDecimal vlBruto) { this.vlBruto = vlBruto; }

    public BigDecimal getVlLiquido() { return vlLiquido; }
    public void setVlLiquido(BigDecimal vlLiquido) { this.vlLiquido = vlLiquido; }

    public String getDsModalidadePag() { return dsModalidadePag; }
    public void setDsModalidadePag(String dsModalidadePag) { this.dsModalidadePag = dsModalidadePag; }

    public String getNuProcesso() { return nuProcesso; }
    public void setNuProcesso(String nuProcesso) { this.nuProcesso = nuProcesso; }

    public String getCdElemento() { return cdElemento; }
    public void setCdElemento(String cdElemento) { this.cdElemento = cdElemento; }

    public String getDescricaoElemento() { return descricaoElemento; }
    public void setDescricaoElemento(String descricaoElemento) { this.descricaoElemento = descricaoElemento; }
}

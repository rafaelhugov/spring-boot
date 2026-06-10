package mppb.restapi.mobile.feature.processodetalhe.dto;

import java.util.Date;

public class MovimentoDTO {
    private Date data;
    private String tipo;
    private String complemento;

    // Getters and Setters
    public Date getData() { return data; }
    public void setData(Date data) { this.data = data; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
}

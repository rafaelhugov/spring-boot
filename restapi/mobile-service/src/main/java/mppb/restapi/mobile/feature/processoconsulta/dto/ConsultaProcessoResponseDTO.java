package mppb.restapi.mobile.feature.processoconsulta.dto;

import java.util.Date;

public class ConsultaProcessoResponseDTO {

    private Long id;
    private String numeroProcesso;
    private String numeroOrigem;
    private String classe;
    private String interessado;
    private String promotoria;
    private String status;
    private Date dataRegistro;
    
    // Construtores
    public ConsultaProcessoResponseDTO() {}

    public ConsultaProcessoResponseDTO(Long id, String numeroProcesso, String numeroOrigem, String classe, String interessado, String promotoria, String status, Date dataRegistro) {
        this.id = id;
        this.numeroProcesso = numeroProcesso;
        this.numeroOrigem = numeroOrigem;
        this.classe = classe;
        this.interessado = interessado;
        this.promotoria = promotoria;
        this.status = status;
        this.dataRegistro = dataRegistro;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public String getNumeroOrigem() {
        return numeroOrigem;
    }

    public void setNumeroOrigem(String numeroOrigem) {
        this.numeroOrigem = numeroOrigem;
    }

    public String getClasse() {
        return classe;
    }

    public void setClasse(String classe) {
        this.classe = classe;
    }

    public String getInteressado() {
        return interessado;
    }

    public void setInteressado(String interessado) {
        this.interessado = interessado;
    }

    public String getPromotoria() {
        return promotoria;
    }

    public void setPromotoria(String promotoria) {
        this.promotoria = promotoria;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(Date dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}

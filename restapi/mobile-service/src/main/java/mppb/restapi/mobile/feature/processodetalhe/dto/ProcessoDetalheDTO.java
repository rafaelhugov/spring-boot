package mppb.restapi.mobile.feature.processodetalhe.dto;

import java.util.Date;
import java.util.List;

public class ProcessoDetalheDTO {
    private Long id;
    private String numeroProcesso;
    private String numeroOrigem;
    private String classe;
    private String assuntoPrincipal;
    private String promotoria;
    private Date dataRegistro;
    private String textoInformado;
    
    private List<InteressadoDTO> interessados;
    private List<MovimentoDTO> movimentos;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroProcesso() { return numeroProcesso; }
    public void setNumeroProcesso(String numeroProcesso) { this.numeroProcesso = numeroProcesso; }
    public String getNumeroOrigem() { return numeroOrigem; }
    public void setNumeroOrigem(String numeroOrigem) { this.numeroOrigem = numeroOrigem; }
    public String getClasse() { return classe; }
    public void setClasse(String classe) { this.classe = classe; }
    public String getAssuntoPrincipal() { return assuntoPrincipal; }
    public void setAssuntoPrincipal(String assuntoPrincipal) { this.assuntoPrincipal = assuntoPrincipal; }
    public String getPromotoria() { return promotoria; }
    public void setPromotoria(String promotoria) { this.promotoria = promotoria; }
    public Date getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(Date dataRegistro) { this.dataRegistro = dataRegistro; }
    public String getTextoInformado() { return textoInformado; }
    public void setTextoInformado(String textoInformado) { this.textoInformado = textoInformado; }
    public List<InteressadoDTO> getInteressados() { return interessados; }
    public void setInteressados(List<InteressadoDTO> interessados) { this.interessados = interessados; }
    public List<MovimentoDTO> getMovimentos() { return movimentos; }
    public void setMovimentos(List<MovimentoDTO> movimentos) { this.movimentos = movimentos; }
}

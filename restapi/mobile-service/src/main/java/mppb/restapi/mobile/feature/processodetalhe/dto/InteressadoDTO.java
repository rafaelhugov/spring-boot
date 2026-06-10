package mppb.restapi.mobile.feature.processodetalhe.dto;

public class InteressadoDTO {
    private String tipoParte;
    private String nome;
    private String nomeSocial;
    private String documento;
    private String tipoPessoa;

    // Getters and Setters
    public String getTipoParte() { return tipoParte; }
    public void setTipoParte(String tipoParte) { this.tipoParte = tipoParte; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getNomeSocial() { return nomeSocial; }
    public void setNomeSocial(String nomeSocial) { this.nomeSocial = nomeSocial; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getTipoPessoa() { return tipoPessoa; }
    public void setTipoPessoa(String tipoPessoa) { this.tipoPessoa = tipoPessoa; }
}

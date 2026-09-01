package mppb.restapi.mobile.feature.processoconsulta.dto;

public class ConsultaProcessoFilterDTO {

    private String numeroProcesso;
    private String numeroOrigem;
    private String numeroCnmp;
    private String cpf;
    private String cnpj;
    private String interessado;
    private Long idComarca;
    private Long idClasse;
    private String status;
    private String nomePromotor;
    private String idTipoAtividade;
    
    // Captcha
    private String captchaToken;

    // Getters and Setters

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

    public String getNumeroCnmp() {
        return numeroCnmp;
    }

    public void setNumeroCnmp(String numeroCnmp) {
        this.numeroCnmp = numeroCnmp;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getInteressado() {
        return interessado;
    }

    public void setInteressado(String interessado) {
        this.interessado = interessado;
    }

    public Long getIdComarca() {
        return idComarca;
    }

    public void setIdComarca(Long idComarca) {
        this.idComarca = idComarca;
    }

    public Long getIdClasse() {
        return idClasse;
    }

    public void setIdClasse(Long idClasse) {
        this.idClasse = idClasse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNomePromotor() {
        return nomePromotor;
    }

    public void setNomePromotor(String nomePromotor) {
        this.nomePromotor = nomePromotor;
    }

    public String getIdTipoAtividade() {
        return idTipoAtividade;
    }

    public void setIdTipoAtividade(String idTipoAtividade) {
        this.idTipoAtividade = idTipoAtividade;
    }

    public String getCaptchaToken() {
        return captchaToken;
    }

    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }
}

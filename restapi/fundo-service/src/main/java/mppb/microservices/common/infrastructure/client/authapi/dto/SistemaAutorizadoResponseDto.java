package mppb.microservices.common.infrastructure.client.authapi.dto;


import java.io.Serializable;

public class SistemaAutorizadoResponseDto implements Serializable {

    private static final long serialVersionUID = 2606909316469540048L;

    private Long id;

    private String descricao;

    private String nome;

    private String sigla;

    private String url;

    public SistemaAutorizadoResponseDto() {
        super();
    }

    public SistemaAutorizadoResponseDto(Long id, String descricao, String nome, String sigla, String url) {
        super();
        this.id = id;
        this.descricao = descricao;
        this.nome = nome;
        this.sigla = sigla;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return String.format("SistemaAutorizadoResponseDto [id=%s, descricao=%s, nome=%s, sigla=%s, url=%s]", id, descricao, nome, sigla,
                url);
    }

}

package mppb.restapi.common.infrastructure.client.authapi.dto;


import java.io.Serializable;

public class PerfilAutorizadoResponseDto implements Serializable {

    private static final long serialVersionUID = 1929559866497831879L;

    private Long id;

    private String nome;

    public PerfilAutorizadoResponseDto() {
        super();
    }

    public PerfilAutorizadoResponseDto(Long id, String nome) {
        super();
        this.id = id;
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public String toString() {
        return String.format("PerfilAutorizadoResponseDto [id=%s, nome=%s]", id, nome);
    }

}

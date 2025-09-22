package mppb.microservices.common.infrastructure.client.authapi.dto;


import java.io.Serializable;

public class FuncionalidadeAutorizadaResponseDto implements Serializable {

    private static final long serialVersionUID = 547966521561303936L;

    private Long id;

    private String nome;

    private String servico;

    public FuncionalidadeAutorizadaResponseDto() {
        super();
    }

    public FuncionalidadeAutorizadaResponseDto(Long id, String nome, String servico) {
        super();
        this.id = id;
        this.nome = nome;
        this.servico = servico;
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

    public String getServico() {
        return servico;
    }

    public void setServico(String servico) {
        this.servico = servico;
    }

    @Override
    public String toString() {
        return String.format("FuncionalidadeAutorizadaResponseDto [id=%s, nome=%s, servico=%s]", id, nome, servico);
    }

}

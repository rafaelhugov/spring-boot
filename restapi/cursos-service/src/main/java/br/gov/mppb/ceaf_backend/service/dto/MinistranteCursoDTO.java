package br.gov.mppb.ceaf_backend.service.dto;

import br.gov.mppb.ceaf_backend.domain.PapelMinistrante;

public class MinistranteCursoDTO {
    private Long id; // ID da associação
    private Long ministranteId; // ID do ministrante
    private Long idServidor;
    private String nome;
    private String nomeOrgaoExterno;
    private String tipo;
    private PapelMinistrante papel;

    // Construtor, Getters e Setters
    public MinistranteCursoDTO(Long id, Long ministranteId, Long idServidor, String nome, String nomeOrgaoExterno, PapelMinistrante papel) {
        this.id = id;
        this.ministranteId = ministranteId;
        this.idServidor = idServidor;
        this.nome = nome;
        this.nomeOrgaoExterno = nomeOrgaoExterno;
        this.papel = papel;
        this.tipo = (idServidor != null) ? "Interno" : "Externo";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMinistranteId() { return ministranteId; }
    public void setMinistranteId(Long ministranteId) { this.ministranteId = ministranteId; }
    public Long getIdServidor() { return idServidor; }
    public void setIdServidor(Long idServidor) { this.idServidor = idServidor; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getNomeOrgaoExterno() { return nomeOrgaoExterno; }
    public void setNomeOrgaoExterno(String nomeOrgaoExterno) { this.nomeOrgaoExterno = nomeOrgaoExterno; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public PapelMinistrante getPapel() { return papel; }
    public void setPapel(PapelMinistrante papel) { this.papel = papel; }
}


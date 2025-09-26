package br.gov.mppb.ceaf_backend.service.dto;

// DTO para RECEBER os dados ao EDITAR a associação de um ministrante
public class MinistranteUpdateDTO {

    private String nome; // Apenas para externos
    private String nomeOrgaoExterno; // Apenas para externos
    private PapelIdDTO papel; // Para ambos

    // Getters e Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNomeOrgaoExterno() {
        return nomeOrgaoExterno;
    }

    public void setNomeOrgaoExterno(String nomeOrgaoExterno) {
        this.nomeOrgaoExterno = nomeOrgaoExterno;
    }

    public PapelIdDTO getPapel() {
        return papel;
    }

    public void setPapel(PapelIdDTO papel) {
        this.papel = papel;
    }

    public static class PapelIdDTO {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}


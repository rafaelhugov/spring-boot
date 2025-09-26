package br.gov.mppb.ceaf_backend.service.dto;

// DTO para RECEBER os dados ao adicionar um ministrante pela nova tela
public class MinistranteAddDTO {

    private Long idServidor;
    private String nome;
    private PapelIdDTO papel;

    // Getters e Setters
    public Long getIdServidor() { return idServidor; }
    public void setIdServidor(Long idServidor) { this.idServidor = idServidor; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public PapelIdDTO getPapel() { return papel; }
    public void setPapel(PapelIdDTO papel) { this.papel = papel; }

    public static class PapelIdDTO {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}


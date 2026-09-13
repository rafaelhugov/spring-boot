package mppb.restapi.mobile.feature.perfil.dto;

import java.util.Date;

import lombok.Data;

@Data
public class PerfilUsuarioDto {
    private String login;
    private String nome;
    private String matricula;
    private String tipoCargo;
    private String cargo;
    private String tituloCargo;
    private String especialidade;
    private String situacao;
    private Date dataPosse;
    private Date dataAdmissao;
    private Date dataExercicio;
    private String email;
    private String telefone;
    private String celular;
    private String setorNome;
    private String setorSigla;
    private String orgaoNome;
    private String orgaoSigla;
}

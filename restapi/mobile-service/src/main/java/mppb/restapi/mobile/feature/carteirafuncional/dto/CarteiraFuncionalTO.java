package mppb.restapi.cursos.feature.carteirafuncional.dto;

import lombok.Data;
import java.util.Date;

@Data
public class CarteiraFuncionalTO {
    private String nome;
    private String rg;
    private String orgaoExp;
    private String cpf;
    private String login;
    private String especialidade;
    private String titulo;
    private String matricula;
    private String naturalidade;
    private Date nascimento;
    private String nomePai;
    private String nomeMae;
    private String cargo;
    private String tipoCargo;
    private String md5;
    private Long idPessoa;
    private Long idServidor;
    private String nomePgj;
    private String tituloCargo;
}

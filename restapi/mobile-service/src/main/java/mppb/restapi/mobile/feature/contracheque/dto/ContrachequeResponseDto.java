package mppb.restapi.mobile.feature.contracheque.dto;

import java.math.BigDecimal;
import java.util.List;

public class ContrachequeResponseDto {
    private String nome;
    private String matricula;
    private String cargo;
    private String mesAno;
    private List<LancamentoDto> lancamentos;
    private BigDecimal totalVantagens;
    private BigDecimal totalDescontos;
    private BigDecimal salarioLiquido;
    private String codigoValidacao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getMesAno() { return mesAno; }
    public void setMesAno(String mesAno) { this.mesAno = mesAno; }

    public List<LancamentoDto> getLancamentos() { return lancamentos; }
    public void setLancamentos(List<LancamentoDto> lancamentos) { this.lancamentos = lancamentos; }

    public BigDecimal getTotalVantagens() { return totalVantagens; }
    public void setTotalVantagens(BigDecimal totalVantagens) { this.totalVantagens = totalVantagens; }

    public BigDecimal getTotalDescontos() { return totalDescontos; }
    public void setTotalDescontos(BigDecimal totalDescontos) { this.totalDescontos = totalDescontos; }

    public BigDecimal getSalarioLiquido() { return salarioLiquido; }
    public void setSalarioLiquido(BigDecimal salarioLiquido) { this.salarioLiquido = salarioLiquido; }

    public String getCodigoValidacao() { return codigoValidacao; }
    public void setCodigoValidacao(String codigoValidacao) { this.codigoValidacao = codigoValidacao; }
}

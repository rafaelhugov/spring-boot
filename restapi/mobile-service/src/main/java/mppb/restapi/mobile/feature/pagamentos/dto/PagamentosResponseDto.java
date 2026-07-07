package mppb.restapi.mobile.feature.pagamentos.dto;

import java.math.BigDecimal;
import java.util.List;

public class PagamentosResponseDto {

    private String nuExercicio;
    private BigDecimal totalBruto;
    private BigDecimal totalLiquido;
    private int totalPagamentos;
    private List<PagamentoItemDto> pagamentos;

    public String getNuExercicio() { return nuExercicio; }
    public void setNuExercicio(String nuExercicio) { this.nuExercicio = nuExercicio; }

    public BigDecimal getTotalBruto() { return totalBruto; }
    public void setTotalBruto(BigDecimal totalBruto) { this.totalBruto = totalBruto; }

    public BigDecimal getTotalLiquido() { return totalLiquido; }
    public void setTotalLiquido(BigDecimal totalLiquido) { this.totalLiquido = totalLiquido; }

    public int getTotalPagamentos() { return totalPagamentos; }
    public void setTotalPagamentos(int totalPagamentos) { this.totalPagamentos = totalPagamentos; }

    public List<PagamentoItemDto> getPagamentos() { return pagamentos; }
    public void setPagamentos(List<PagamentoItemDto> pagamentos) { this.pagamentos = pagamentos; }
}

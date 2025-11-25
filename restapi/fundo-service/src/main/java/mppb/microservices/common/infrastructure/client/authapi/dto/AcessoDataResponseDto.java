package mppb.microservices.common.infrastructure.client.authapi.dto;

import java.io.Serializable;
import java.util.List;

public class AcessoDataResponseDto implements Serializable {

    private static final long serialVersionUID = -2489538217535499929L;

    private List<FuncionalidadeAutorizadaResponseDto> funcionalidadesNegadas;

    private List<FuncionalidadeAutorizadaResponseDto> funcionalidadesPermitidas;

    private List<PerfilAutorizadoResponseDto> perfis;

    private List<SistemaAutorizadoResponseDto> sistemas;

    public AcessoDataResponseDto() {
        super();
    }

    public AcessoDataResponseDto(List<FuncionalidadeAutorizadaResponseDto> funcionalidadesNegadas,
                                 List<FuncionalidadeAutorizadaResponseDto> funcionalidadesPermitidas, List<PerfilAutorizadoResponseDto> perfis,
                                 List<SistemaAutorizadoResponseDto> sistemas) {
        super();
        this.funcionalidadesNegadas = funcionalidadesNegadas;
        this.funcionalidadesPermitidas = funcionalidadesPermitidas;
        this.perfis = perfis;
        this.sistemas = sistemas;
    }

    public List<FuncionalidadeAutorizadaResponseDto> getFuncionalidadesNegadas() {
        return funcionalidadesNegadas;
    }

    public void setFuncionalidadesNegadas(List<FuncionalidadeAutorizadaResponseDto> funcionalidadesNegadas) {
        this.funcionalidadesNegadas = funcionalidadesNegadas;
    }

    public List<FuncionalidadeAutorizadaResponseDto> getFuncionalidadesPermitidas() {
        return funcionalidadesPermitidas;
    }

    public void setFuncionalidadesPermitidas(List<FuncionalidadeAutorizadaResponseDto> funcionalidadesPermitidas) {
        this.funcionalidadesPermitidas = funcionalidadesPermitidas;
    }

    public List<PerfilAutorizadoResponseDto> getPerfis() {
        return perfis;
    }

    public void setPerfis(List<PerfilAutorizadoResponseDto> perfis) {
        this.perfis = perfis;
    }

    public List<SistemaAutorizadoResponseDto> getSistemas() {
        return sistemas;
    }

    public void setSistemas(List<SistemaAutorizadoResponseDto> sistemas) {
        this.sistemas = sistemas;
    }

    @Override
    public String toString() {
        return String.format("AcessoDataResponseDto [funcionalidadesNegadas=%s, funcionalidadesPermitidas=%s, perfis=%s, sistemas=%s]",
                funcionalidadesNegadas, funcionalidadesPermitidas, perfis, sistemas);
    }

}

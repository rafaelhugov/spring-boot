package mppb.microservices.common.client.authapi.dto;

import java.io.Serializable;
import java.util.List;

public class AcessoDataResponseTO implements Serializable {

    private static final long serialVersionUID = -2489538217535499929L;

    private List<FuncionalidadeAutorizadaResponseTO> funcionalidadesNegadas;

    private List<FuncionalidadeAutorizadaResponseTO> funcionalidadesPermitidas;

    private List<PerfilAutorizadoResponseTO> perfis;

    private List<SistemaAutorizadoResponseTO> sistemas;

    public AcessoDataResponseTO() {
        super();
    }

    public AcessoDataResponseTO(List<FuncionalidadeAutorizadaResponseTO> funcionalidadesNegadas,
                                List<FuncionalidadeAutorizadaResponseTO> funcionalidadesPermitidas, List<PerfilAutorizadoResponseTO> perfis,
                                List<SistemaAutorizadoResponseTO> sistemas) {
        super();
        this.funcionalidadesNegadas = funcionalidadesNegadas;
        this.funcionalidadesPermitidas = funcionalidadesPermitidas;
        this.perfis = perfis;
        this.sistemas = sistemas;
    }

    public List<FuncionalidadeAutorizadaResponseTO> getFuncionalidadesNegadas() {
        return funcionalidadesNegadas;
    }

    public void setFuncionalidadesNegadas(List<FuncionalidadeAutorizadaResponseTO> funcionalidadesNegadas) {
        this.funcionalidadesNegadas = funcionalidadesNegadas;
    }

    public List<FuncionalidadeAutorizadaResponseTO> getFuncionalidadesPermitidas() {
        return funcionalidadesPermitidas;
    }

    public void setFuncionalidadesPermitidas(List<FuncionalidadeAutorizadaResponseTO> funcionalidadesPermitidas) {
        this.funcionalidadesPermitidas = funcionalidadesPermitidas;
    }

    public List<PerfilAutorizadoResponseTO> getPerfis() {
        return perfis;
    }

    public void setPerfis(List<PerfilAutorizadoResponseTO> perfis) {
        this.perfis = perfis;
    }

    public List<SistemaAutorizadoResponseTO> getSistemas() {
        return sistemas;
    }

    public void setSistemas(List<SistemaAutorizadoResponseTO> sistemas) {
        this.sistemas = sistemas;
    }

    @Override
    public String toString() {
        return String.format("AcessoDataResponseTO [funcionalidadesNegadas=%s, funcionalidadesPermitidas=%s, perfis=%s, sistemas=%s]",
                funcionalidadesNegadas, funcionalidadesPermitidas, perfis, sistemas);
    }

}

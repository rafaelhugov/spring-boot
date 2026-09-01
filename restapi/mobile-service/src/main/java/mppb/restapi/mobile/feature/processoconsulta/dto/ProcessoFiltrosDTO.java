package mppb.restapi.mobile.feature.processoconsulta.dto;

import java.util.List;

public class ProcessoFiltrosDTO {

    private List<ProcessoFiltroOptionDTO> comarcas;
    private List<ProcessoFiltroOptionDTO> classes;
    private List<ProcessoFiltroOptionDTO> tiposAtividade;
    private List<ProcessoFiltroOptionDTO> status;

    public ProcessoFiltrosDTO(
            List<ProcessoFiltroOptionDTO> comarcas,
            List<ProcessoFiltroOptionDTO> classes,
            List<ProcessoFiltroOptionDTO> tiposAtividade,
            List<ProcessoFiltroOptionDTO> status) {
        this.comarcas = comarcas;
        this.classes = classes;
        this.tiposAtividade = tiposAtividade;
        this.status = status;
    }

    public List<ProcessoFiltroOptionDTO> getComarcas() {
        return comarcas;
    }

    public void setComarcas(List<ProcessoFiltroOptionDTO> comarcas) {
        this.comarcas = comarcas;
    }

    public List<ProcessoFiltroOptionDTO> getClasses() {
        return classes;
    }

    public void setClasses(List<ProcessoFiltroOptionDTO> classes) {
        this.classes = classes;
    }

    public List<ProcessoFiltroOptionDTO> getTiposAtividade() {
        return tiposAtividade;
    }

    public void setTiposAtividade(List<ProcessoFiltroOptionDTO> tiposAtividade) {
        this.tiposAtividade = tiposAtividade;
    }

    public List<ProcessoFiltroOptionDTO> getStatus() {
        return status;
    }

    public void setStatus(List<ProcessoFiltroOptionDTO> status) {
        this.status = status;
    }
}

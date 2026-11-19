package mppb.restapi.mobile.feature.mensagem.dto;

import java.util.List;

import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository.OpcaoGrupoRh;

public record CatalogoGruposRhResponse(
        List<OpcaoGrupoRh> tiposCargo,
        List<OpcaoGrupoRh> orgaos,
        List<OpcaoGrupoRh> setores,
        List<OpcaoGrupoRh> cargos) {
}


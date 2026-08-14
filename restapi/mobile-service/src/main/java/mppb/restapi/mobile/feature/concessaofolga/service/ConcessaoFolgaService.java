package mppb.restapi.mobile.feature.concessaofolga.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.repository.ConcessaoFolgaIdentityRepository;
import mppb.restapi.mobile.feature.concessaofolga.repository.ConcessaoFolgaRemoteRepository;

@Service
@RequiredArgsConstructor
public class ConcessaoFolgaService {

    private final ConcessaoFolgaIdentityRepository identityRepository;
    private final ConcessaoFolgaRemoteRepository remoteRepository;

    public String getMatriculaByLogin(String login) {
        return identityRepository.getMatriculaByLogin(login);
    }

    public List<ConcessaoFolgaDto> listarConcessoes(String matricula) {
        return remoteRepository.findConcessoesVigentes(matricula);
    }

    public List<AusenciaCalendarioDto> listarAusencias(String matricula, Integer mes, Integer ano) {
        if (mes != null && (mes < 1 || mes > 12)) {
            throw new IllegalArgumentException("Mes invalido");
        }
        if (ano != null && (ano < 1900 || ano > 2200)) {
            throw new IllegalArgumentException("Ano invalido");
        }
        return remoteRepository.findAusencias(matricula, mes, ano);
    }
}

package mppb.restapi.mobile.feature.ferias.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.ferias.dto.EditalFeriasDto;
import mppb.restapi.mobile.feature.ferias.dto.NovaSolicitacaoFeriasDto;
import mppb.restapi.mobile.feature.ferias.dto.SolicitacaoFLADto;
import mppb.restapi.mobile.feature.ferias.repository.FeriasRepository;

@Service
@RequiredArgsConstructor
public class FeriasService {

    private final FeriasRepository feriasRepository;

    public Integer getMatriculaByLogin(String login) {
        return feriasRepository.getMatriculaByLogin(login);
    }
    
    public Long getIdServidorByMatricula(Integer matricula) {
        return feriasRepository.getIdServidorByMatricula(matricula);
    }
    
    public Long getIdUsuarioByLogin(String login) {
        return feriasRepository.getIdUsuarioByLogin(login);
    }

    public List<SolicitacaoFLADto> listarMinhasSolicitacoes(Integer matricula) {
        return feriasRepository.findSolicitacoesByMatricula(matricula);
    }

    public List<EditalFeriasDto> listarEditaisAbertos() {
        return feriasRepository.findEditaisAbertos();
    }

    public void solicitarFerias(NovaSolicitacaoFeriasDto dto, Integer matricula, String login) {
        if (dto.getDataInicio() == null || dto.getDataFim() == null || dto.getTipo() == null) {
            throw new IllegalArgumentException("Dados incompletos para solicitação de férias");
        }
        
        Long idServidor = feriasRepository.getIdServidorByMatricula(matricula);
        if (idServidor == null) {
            throw new IllegalArgumentException("Servidor não encontrado");
        }
        
        Long idUsuario = feriasRepository.getIdUsuarioByLogin(login);
        if (idUsuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        
        feriasRepository.saveSolicitacao(dto, idServidor, idUsuario);
    }
}

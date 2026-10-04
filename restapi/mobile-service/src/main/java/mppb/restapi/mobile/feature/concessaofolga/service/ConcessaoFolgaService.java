package mppb.restapi.mobile.feature.concessaofolga.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.OperacaoSolicitacaoRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoMarcacaoFeriasRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.repository.ConcessaoFolgaIdentityRepository;
import mppb.restapi.mobile.feature.concessaofolga.repository.ConcessaoFolgaRemoteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import mppb.restapi.mobile.feature.synthetic.dataset.SyntheticDataService;
import mppb.restapi.mobile.feature.synthetic.sandbox.SyntheticSandboxStorage;

@Service
public class ConcessaoFolgaService {

    private final ConcessaoFolgaIdentityRepository identityRepository;
    private final ConcessaoFolgaRemoteRepository remoteRepository;
    private final MpVirtualConcessaoFolgaClient mpVirtualClient;

    private SyntheticDataService syntheticDataService;
    private SyntheticSandboxStorage sandboxStorage;

    public ConcessaoFolgaService(
            ConcessaoFolgaIdentityRepository identityRepository,
            ConcessaoFolgaRemoteRepository remoteRepository,
            MpVirtualConcessaoFolgaClient mpVirtualClient) {
        this.identityRepository = identityRepository;
        this.remoteRepository = remoteRepository;
        this.mpVirtualClient = mpVirtualClient;
    }

    @Autowired
    public ConcessaoFolgaService(
            ConcessaoFolgaIdentityRepository identityRepository,
            ConcessaoFolgaRemoteRepository remoteRepository,
            MpVirtualConcessaoFolgaClient mpVirtualClient,
            @Autowired(required = false) SyntheticDataService syntheticDataService,
            @Autowired(required = false) SyntheticSandboxStorage sandboxStorage) {
        this.identityRepository = identityRepository;
        this.remoteRepository = remoteRepository;
        this.mpVirtualClient = mpVirtualClient;
        this.syntheticDataService = syntheticDataService;
        this.sandboxStorage = sandboxStorage;
    }

    public String getMatriculaByLogin(String login) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticLogin(login)) {
            return syntheticDataService.getMatriculaByLogin(login);
        }
        return identityRepository.getMatriculaByLogin(login);
    }

    public List<ConcessaoFolgaDto> listarConcessoes(String matricula) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticMatricula(matricula) && sandboxStorage != null) {
            return sandboxStorage.listarConcessoes(matricula);
        }
        return remoteRepository.findConcessoesVigentes(matricula);
    }

    public List<AusenciaCalendarioDto> listarAusencias(String matricula, Integer mes, Integer ano) {
        if (mes != null && (mes < 1 || mes > 12)) {
            throw new IllegalArgumentException("Mes invalido");
        }
        if (ano != null && (ano < 1900 || ano > 2200)) {
            throw new IllegalArgumentException("Ano invalido");
        }
        if (syntheticDataService != null && syntheticDataService.isSyntheticMatricula(matricula) && sandboxStorage != null) {
            return sandboxStorage.listarAusencias(matricula, mes, ano);
        }
        return remoteRepository.findAusencias(matricula, mes, ano);
    }

    public ResponseEntity<String> sugestoesDatasFinais(Long concessaoId, String dataInicial, String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            return sandboxStorage.sugestoesDatasFinais(concessaoId, dataInicial);
        }
        return mpVirtualClient.sugestoesDatasFinais(concessaoId, dataInicial, token);
    }

    public ResponseEntity<String> cadastrarSolicitacao(
            Long concessaoId,
            SolicitacaoMarcacaoFeriasRequestDto request,
            String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            return sandboxStorage.cadastrarSolicitacao(concessaoId, request, token);
        }
        return mpVirtualClient.cadastrarSolicitacao(concessaoId, request, token);
    }

    public ResponseEntity<String> consultarSolicitacao(Long solicitacaoId, String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            String matricula = syntheticDataService.getMatriculaByTokenOrLogin(token);
            return sandboxStorage.consultarSolicitacao(solicitacaoId, matricula);
        }
        return mpVirtualClient.consultarSolicitacao(solicitacaoId, token);
    }

    public ResponseEntity<String> consultarOperacoesSolicitacao(Long solicitacaoId, String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            String matricula = syntheticDataService.getMatriculaByTokenOrLogin(token);
            return sandboxStorage.consultarOperacoesSolicitacao(solicitacaoId, matricula);
        }
        return mpVirtualClient.consultarOperacoesSolicitacao(solicitacaoId, token);
    }

    public ResponseEntity<String> cancelarSolicitacao(Long solicitacaoId, String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            String matricula = syntheticDataService.getMatriculaByTokenOrLogin(token);
            return sandboxStorage.cancelarSolicitacao(solicitacaoId, matricula);
        }
        return mpVirtualClient.cancelarSolicitacao(solicitacaoId, token);
    }

    public ResponseEntity<String> adiarSolicitacao(
            Long solicitacaoId,
            OperacaoSolicitacaoRequestDto request,
            String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            String matricula = syntheticDataService.getMatriculaByTokenOrLogin(token);
            return sandboxStorage.adiarSolicitacao(solicitacaoId, request, matricula);
        }
        return mpVirtualClient.adiarSolicitacao(solicitacaoId, request, token);
    }

    public ResponseEntity<String> anteciparSolicitacao(
            Long solicitacaoId,
            OperacaoSolicitacaoRequestDto request,
            String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            String matricula = syntheticDataService.getMatriculaByTokenOrLogin(token);
            return sandboxStorage.anteciparSolicitacao(solicitacaoId, request, matricula);
        }
        return mpVirtualClient.anteciparSolicitacao(solicitacaoId, request, token);
    }

    public ResponseEntity<String> interromperSolicitacao(
            Long solicitacaoId,
            OperacaoSolicitacaoRequestDto request,
            String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            String matricula = syntheticDataService.getMatriculaByTokenOrLogin(token);
            return sandboxStorage.interromperSolicitacao(solicitacaoId, request, matricula);
        }
        return mpVirtualClient.interromperSolicitacao(solicitacaoId, request, token);
    }

    public ResponseEntity<String> suspenderIntegralmenteSolicitacao(Long solicitacaoId, String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            String matricula = syntheticDataService.getMatriculaByTokenOrLogin(token);
            return sandboxStorage.suspenderIntegralmenteSolicitacao(solicitacaoId, matricula);
        }
        return mpVirtualClient.suspenderIntegralmenteSolicitacao(solicitacaoId, token);
    }

    public ResponseEntity<String> suspenderParcialmenteSolicitacao(
            Long solicitacaoId,
            OperacaoSolicitacaoRequestDto request,
            String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticTokenOrLogin(token) && sandboxStorage != null) {
            String matricula = syntheticDataService.getMatriculaByTokenOrLogin(token);
            return sandboxStorage.suspenderParcialmenteSolicitacao(solicitacaoId, request, matricula);
        }
        return mpVirtualClient.suspenderParcialmenteSolicitacao(solicitacaoId, request, token);
    }
}

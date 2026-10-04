package mppb.restapi.mobile.feature.designacao.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoIdentityRepository;

import org.springframework.beans.factory.annotation.Autowired;
import mppb.restapi.mobile.feature.synthetic.dataset.SyntheticDataService;

@Slf4j
@Service
public class DesignacaoAuthService {

    private final AuthServiceClient authServiceClient;
    private final DesignacaoIdentityRepository identityRepository;

    private SyntheticDataService syntheticDataService;

    @Value("${sistema.sigla:MPVIRTUAL}")
    private String siglaSistema;

    public DesignacaoAuthService(AuthServiceClient authServiceClient, DesignacaoIdentityRepository identityRepository) {
        this.authServiceClient = authServiceClient;
        this.identityRepository = identityRepository;
    }

    @Autowired
    public DesignacaoAuthService(AuthServiceClient authServiceClient, DesignacaoIdentityRepository identityRepository,
                                 @Autowired(required = false) SyntheticDataService syntheticDataService) {
        this.authServiceClient = authServiceClient;
        this.identityRepository = identityRepository;
        this.syntheticDataService = syntheticDataService;
    }

    public String matriculaAutenticada(String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticToken(token)) {
            return syntheticDataService.getMatriculaByToken(token);
        }
        String login = loginAutenticado(token);
        if (login == null) {
            return null;
        }
        return matriculaPorLogin(login);
    }

    public String matriculaPorLogin(String login) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticLogin(login)) {
            return syntheticDataService.getMatriculaByLogin(login);
        }
        String matricula = identityRepository.getMatriculaByLogin(login);
        return matricula == null || matricula.isBlank() ? null : matricula;
    }

    private String loginAutenticado(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            ConsultaAutorizacaoRequestDto req = new ConsultaAutorizacaoRequestDto(token, siglaSistema);
            ConsultaAutorizacaoResponseDto autorizacao = authServiceClient.consultaAutorizacao(req);
            if (autorizacao == null || autorizacao.getUsuario() == null) {
                return null;
            }
            return autorizacao.getUsuario().getLogin();
        } catch (Exception e) {
            log.warn("Falha ao consultar autorizacao para designacoes");
            return null;
        }
    }
}

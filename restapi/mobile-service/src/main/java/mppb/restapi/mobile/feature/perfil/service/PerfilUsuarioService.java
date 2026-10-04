package mppb.restapi.mobile.feature.perfil.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import mppb.restapi.mobile.feature.synthetic.dataset.SyntheticDataService;

@Slf4j
@Service
public class PerfilUsuarioService {

    private final AuthServiceClient authServiceClient;
    private final PerfilUsuarioRepository repository;

    private SyntheticDataService syntheticDataService;

    @Value("${sistema.sigla:MPVIRTUAL}")
    private String siglaSistema;

    public PerfilUsuarioService(AuthServiceClient authServiceClient, PerfilUsuarioRepository repository) {
        this.authServiceClient = authServiceClient;
        this.repository = repository;
    }

    @Autowired
    public PerfilUsuarioService(AuthServiceClient authServiceClient, PerfilUsuarioRepository repository,
                                @Autowired(required = false) SyntheticDataService syntheticDataService) {
        this.authServiceClient = authServiceClient;
        this.repository = repository;
        this.syntheticDataService = syntheticDataService;
    }

    public PerfilUsuarioDto buscarPerfilAutenticado(String token) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticToken(token)) {
            return syntheticDataService.getPerfilByToken(token);
        }
        String login = loginAutenticado(token);
        if (login == null) {
            return null;
        }
        return repository.buscarPorLogin(login);
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
            log.warn("Falha ao consultar autorizacao para perfil");
            return null;
        }
    }
}

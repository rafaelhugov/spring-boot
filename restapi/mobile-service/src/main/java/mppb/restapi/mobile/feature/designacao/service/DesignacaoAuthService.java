package mppb.restapi.mobile.feature.designacao.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoIdentityRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignacaoAuthService {

    private final AuthServiceClient authServiceClient;
    private final DesignacaoIdentityRepository identityRepository;

    public String matriculaAutenticada(String token) {
        String login = loginAutenticado(token);
        if (login == null) {
            return null;
        }
        String matricula = identityRepository.getMatriculaByLogin(login);
        return matricula == null || matricula.isBlank() ? null : matricula;
    }

    private String loginAutenticado(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            ConsultaAutorizacaoRequestDto req = new ConsultaAutorizacaoRequestDto(token, "ANDROID");
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

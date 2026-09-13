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

@Slf4j
@Service
@RequiredArgsConstructor
public class PerfilUsuarioService {

    private final AuthServiceClient authServiceClient;
    private final PerfilUsuarioRepository repository;

    @Value("${sistema.sigla:MPVIRTUAL}")
    private String siglaSistema;

    public PerfilUsuarioDto buscarPerfilAutenticado(String token) {
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

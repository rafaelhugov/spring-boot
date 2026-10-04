package mppb.restapi.mobile.feature.synthetic.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.FuncionalidadeAutorizadaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import mppb.restapi.mobile.feature.synthetic.audit.AuditoriaUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;

@Slf4j
@Component
@ConditionalOnProperty(name = "uat.synthetic-users.enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SyntheticTokenAuthenticationProvider implements AuthenticationProvider {

    private final AutenticadorUsuarioSintetico autenticador;
    private final AuditoriaUsuarioSintetico auditoria;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof TokenAuthentication tokenAuth)) {
            return null;
        }

        String token = tokenAuth.getToken();
        if (token == null || !autenticador.isHabilitado() || !autenticador.isTokenSintetico(token)) {
            // Nao e token sintetico ou feature desligada -> repassar para TokenAuthenticationProvider padrao
            return null;
        }

        Optional<IdentidadeSintetica> optIdentidade = autenticador.validarToken(token);
        if (optIdentidade.isEmpty()) {
            log.warn("Token sintetico invalido ou expirado rejeitado.");
            throw new BadCredentialsException("security.invalid-credentials");
        }

        IdentidadeSintetica identidade = optIdentidade.get();
        auditoria.registrar(identidade, "VALIDACAO_TOKEN_SUCESSO");

        UsuarioAutorizadoResponseDto usuario = new UsuarioAutorizadoResponseDto(
                1000000L + identidade.getMatriculaNumerica(),
                identidade.getNome(),
                identidade.getLogin());

        List<FuncionalidadeAutorizadaResponseDto> permitidas = new ArrayList<>();
        permitidas.add(new FuncionalidadeAutorizadaResponseDto(1L, "Mobile Acesso", "mobile.acesso"));
        permitidas.add(new FuncionalidadeAutorizadaResponseDto(2L, "Carteira Funcional", "mobile.carteira"));
        permitidas.add(new FuncionalidadeAutorizadaResponseDto(3L, "Contracheque", "mobile.contracheque"));
        permitidas.add(new FuncionalidadeAutorizadaResponseDto(4L, "Pagamentos", "mobile.pagamentos"));
        permitidas.add(new FuncionalidadeAutorizadaResponseDto(5L, "Designacoes", "mobile.designacoes"));
        permitidas.add(new FuncionalidadeAutorizadaResponseDto(6L, "Concessoes", "mobile.concessoes"));

        AcessoDataResponseDto acessoData = new AcessoDataResponseDto(null, permitidas, null, null);

        return new TokenAuthentication(
                token,
                usuario,
                acessoData,
                null,
                SecurityUtils.getAuthorities(acessoData));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TokenAuthentication.class.isAssignableFrom(authentication);
    }
}

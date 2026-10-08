package mppb.restapi.mobile.feature.synthetic.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

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
public class SyntheticTokenAuthenticationProvider implements AuthenticationProvider {

    private final AutenticadorUsuarioSintetico autenticador;
    private final AutenticadorStoreReview autenticadorStoreReview;
    private final AuditoriaUsuarioSintetico auditoria;
    private final Environment environment;

    public SyntheticTokenAuthenticationProvider(
            AutenticadorUsuarioSintetico autenticador,
            AuditoriaUsuarioSintetico auditoria) {
        this(autenticador, null, auditoria, null);
    }

    @Autowired
    public SyntheticTokenAuthenticationProvider(
            AutenticadorUsuarioSintetico autenticador,
            @Autowired(required = false) AutenticadorStoreReview autenticadorStoreReview,
            AuditoriaUsuarioSintetico auditoria,
            @Autowired(required = false) Environment environment) {
        this.autenticador = autenticador;
        this.autenticadorStoreReview = autenticadorStoreReview;
        this.auditoria = auditoria;
        this.environment = environment;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof TokenAuthentication tokenAuth)) {
            return null;
        }

        String token = tokenAuth.getToken();
        if (token == null) {
            return null;
        }

        boolean reviewProfile = isReviewProfile();

        // 1. Tratamento de token Store Review
        if (autenticadorStoreReview != null && autenticadorStoreReview.isTokenReview(token)) {
            Optional<IdentidadeSintetica> optIdentidade = autenticadorStoreReview.validarTokenReview(token);
            if (optIdentidade.isEmpty()) {
                log.warn("Token Store Review invalido ou expirado rejeitado.");
                throw new BadCredentialsException("security.invalid-credentials");
            }
            return criarAuthenticationParaIdentidade(token, optIdentidade.get());
        }

        // 2. Se o backend estiver rodando no perfil exclusivo de Store Review:
        // Tokens UAT e institucionais sao rigorosamente rejeitados
        if (reviewProfile) {
            log.warn("Token nao-review rejeitado no ambiente exclusivo de Store Review.");
            throw new BadCredentialsException("security.invalid-credentials");
        }

        // 3. Tratamento de token sintetico UAT padrao (ambiente interno)
        if (autenticador.isHabilitado() && autenticador.isTokenSintetico(token)) {
            Optional<IdentidadeSintetica> optIdentidade = autenticador.validarToken(token);
            if (optIdentidade.isEmpty()) {
                log.warn("Token sintetico UAT invalido ou expirado rejeitado.");
                throw new BadCredentialsException("security.invalid-credentials");
            }

            IdentidadeSintetica identidade = optIdentidade.get();
            auditoria.registrar(identidade, "VALIDACAO_TOKEN_SUCESSO");
            return criarAuthenticationParaIdentidade(token, identidade);
        }

        // Token nao sintetico e fora do profile review -> repassar para provedor institucional padrao
        return null;
    }

    private Authentication criarAuthenticationParaIdentidade(String token, IdentidadeSintetica identidade) {
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

    private boolean isReviewProfile() {
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .filter(p -> p != null)
                .map(p -> p.trim().toLowerCase(Locale.ROOT))
                .anyMatch("review"::equals);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TokenAuthentication.class.isAssignableFrom(authentication);
    }
}

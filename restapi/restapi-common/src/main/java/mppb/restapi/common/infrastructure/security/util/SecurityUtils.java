package mppb.restapi.common.infrastructure.security.util;


import java.util.*;

import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.FuncionalidadeAutorizadaResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


public class SecurityUtils {

    private static final String CURSOS_ACESSO = "cursos.acesso";

    public static Collection<? extends GrantedAuthority> getAuthorities(AcessoDataResponseDto acessoData) {
        if (acessoData == null || acessoData.getFuncionalidadesPermitidas() == null) {
            return Collections.emptySet();
        }

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        acessoData.getFuncionalidadesPermitidas().stream().forEach(f -> authorities.add(new SimpleGrantedAuthority(f.getServico())));

        return authorities;
    }

    public static void addCursosDefaultPermissions(AcessoDataResponseDto acessoData) {
        if (acessoData == null) {
            return;
        }

        List<FuncionalidadeAutorizadaResponseDto> permitidas = acessoData.getFuncionalidadesPermitidas();
        if (permitidas == null) {
            permitidas = new ArrayList<>();
            acessoData.setFuncionalidadesPermitidas(permitidas);
        }
        List<FuncionalidadeAutorizadaResponseDto> funcionalidadesPermitidas = permitidas;

        boolean hasCursosAccess = funcionalidadesPermitidas.stream()
                .anyMatch(f -> CURSOS_ACESSO.equals(f.getServico()));
        if (!hasCursosAccess) {
            return;
        }

        addPermissionIfMissing(funcionalidadesPermitidas, "Dashboard", "cursos.dashboard");
        addPermissionIfMissing(funcionalidadesPermitidas, "Solicitar", "cursos.solicitar");
    }

    private static void addPermissionIfMissing(List<FuncionalidadeAutorizadaResponseDto> funcionalidadesPermitidas,
                                               String nome,
                                               String servico) {
        boolean alreadyPresent = funcionalidadesPermitidas.stream()
                .anyMatch(f -> servico.equals(f.getServico()));
        if (!alreadyPresent) {
            funcionalidadesPermitidas.add(new FuncionalidadeAutorizadaResponseDto(null, nome, servico));
        }
    }

    /**
     * Obtém o objeto TokenAuthentication do utilizador logado.
     * @return Um Optional contendo o TokenAuthentication.
     */
    public static Optional<TokenAuthentication> getAuthenticationToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof TokenAuthentication) {
            return Optional.of((TokenAuthentication) authentication);
        }

        // Caso especial para modo insecure onde o Spring usa o token padrão
        if (authentication instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken upas = (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) authentication;
            String login = upas.getName();

            // Mapeia para o DTO esperado pelo sistema
            mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto mockUser =
                    new mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto(null, login, login);

            // Cria um TokenAuthentication sintético para compatibilidade
            TokenAuthentication syntheticResult = new TokenAuthentication(null, mockUser, null, upas.getCredentials(), upas.getAuthorities());
            return Optional.of(syntheticResult);
        }

        return Optional.empty();
    }

}

package mppb.microservices.common.infrastructure.security.filter;


import java.io.IOException;

import mppb.microservices.common.infrastructure.security.authorization.UsuarioSenhaAuthentication;
import mppb.microservices.common.infrastructure.security.manager.CustomAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomUsernamePasswordAuthenticationFilter extends OncePerRequestFilter {


    private CustomAuthenticationManager manager;

    public CustomUsernamePasswordAuthenticationFilter(CustomAuthenticationManager manager) {
        this.manager = manager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String basic = recuperarBasic(request);

        if (basic != null) {

            String credentials = new String(java.util.Base64.getDecoder().decode(basic));

            UsuarioSenhaAuthentication authRequest= new UsuarioSenhaAuthentication(credentials);
            Authentication authentication = manager.authenticate(authRequest);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);


//		String basic = recuperarBasic(request);
//
//		if(basic != null) {
//			String credential = new String(java.util.Base64.getDecoder().decode(basic));
//		}
//
//		String response = recuperarToken(request);
//
//		if (response != null) {
//
//			ConsultaAutorizacaoRequestDto param = new ConsultaAutorizacaoRequestDto();
//			param.setToken(response);
//			param.setSiglaSistema(siglaSistema);
//
//			authServiceClient.consultaAutorizacao(null);
//
//			UsuarioAutorizadoResponseDto usuario = new UsuarioAutorizadoResponseDto ();
//			usuario.setId(1L);
//			usuario.setLogin("bruno");
//			usuario.setNome("BRUNOO");
//
//			TokenAuthentication tokenAuthentication = new TokenAuthentication(response, usuario, null, null , null);
//			SecurityContextHolder.getContext().setAuthentication(tokenAuthentication);
//		}
//
//		filterChain.doFilter(request, response);
    }

    private String recuperarBasic(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Basic ")) {
            return authorizationHeader.replace("Basic ", "");
        }

        return null;
    }
//

}
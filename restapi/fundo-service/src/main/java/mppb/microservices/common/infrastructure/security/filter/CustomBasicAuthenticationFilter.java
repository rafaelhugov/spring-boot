package mppb.microservices.common.infrastructure.security.filter;


import java.io.IOException;

import mppb.microservices.common.infrastructure.security.authorization.TokenAuthentication;
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
public class CustomBasicAuthenticationFilter extends OncePerRequestFilter {

    private CustomAuthenticationManager manager;

    public CustomBasicAuthenticationFilter(CustomAuthenticationManager manager) {
        this.manager = manager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = recuperarToken(request);

        if (token != null) {
            TokenAuthentication authRequest= new TokenAuthentication(token);
            Authentication authentication = manager.authenticate(authRequest);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);

    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Token ")) {
            return authorizationHeader.replace("Token ", "");
        }

        return null;
    }

}
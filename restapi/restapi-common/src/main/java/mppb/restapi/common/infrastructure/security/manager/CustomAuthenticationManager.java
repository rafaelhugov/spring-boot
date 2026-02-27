package mppb.restapi.common.infrastructure.security.manager;


import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;

@Component
public class CustomAuthenticationManager implements AuthenticationManager {
    private final ProviderManager providerManager;

    public CustomAuthenticationManager(List<AuthenticationProvider> providers) {
        this.providerManager = new ProviderManager(providers);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return providerManager.authenticate(authentication);
    }
}
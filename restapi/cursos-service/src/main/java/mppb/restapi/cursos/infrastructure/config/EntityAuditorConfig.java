package mppb.restapi.cursos.infrastructure.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import mppb.restapi.common.feature.autorizacao.entities.Usuario;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAware")
@RequiredArgsConstructor
public class EntityAuditorConfig implements AuditorAware<Usuario> {

    private final EntityManager entityManager;

    @Override
    public Optional<Usuario> getCurrentAuditor() {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()  ) {
            return Optional.empty();
        }

        if(authentication instanceof TokenAuthentication) {
            TokenAuthentication tokenAuthention = (TokenAuthentication) authentication;
            UsuarioAutorizadoResponseDto usuarioLogado = (UsuarioAutorizadoResponseDto) tokenAuthention.getUsuario();

            // Usamos o EntityManager para obter uma referência para o Usuario sem consultá-lo
            // Isso é leve e eficiente.
            Usuario usuarioProxy = entityManager.getReference(Usuario.class,  usuarioLogado.getId());
            return Optional.of(usuarioProxy);
        } else {
            return Optional.empty();
        }


    }
}

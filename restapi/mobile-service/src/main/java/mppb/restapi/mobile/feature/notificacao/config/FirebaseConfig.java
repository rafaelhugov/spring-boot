package mppb.restapi.mobile.feature.notificacao.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
@ConditionalOnProperty(prefix = "designacoes.notificacao.fcm", name = "enabled", havingValue = "true")
public class FirebaseConfig {

    static final String APP_NAME = "appmppb";

    @Bean
    @ConditionalOnMissingBean
    FirebaseAdminInitializer firebaseAdminInitializer() {
        return new FirebaseAdminInitializer();
    }

    @Bean
    FirebaseApp firebaseApp(
            FirebaseAdminInitializer initializer,
            @Value("${designacoes.notificacao.fcm.project-id:appmppb}") String projectId) {
        return initializer.initialize(projectId);
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    static class FirebaseAdminInitializer {

        private final FirebaseCredentialsSupplier credentialsSupplier;

        FirebaseAdminInitializer() {
            this(GoogleCredentials::getApplicationDefault);
        }

        FirebaseAdminInitializer(FirebaseCredentialsSupplier credentialsSupplier) {
            this.credentialsSupplier = credentialsSupplier;
        }

        FirebaseApp initialize(String projectId) {
            String projectIdNormalizado = normalizarProjectId(projectId);
            return FirebaseApp.getApps().stream()
                    .filter(app -> APP_NAME.equals(app.getName())
                            || projectIdNormalizado.equals(app.getOptions().getProjectId()))
                    .findFirst()
                    .orElseGet(() -> initializeNewApp(projectIdNormalizado));
        }

        private FirebaseApp initializeNewApp(String projectId) {
            try {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentialsSupplier.get())
                        .setProjectId(projectId)
                        .build();
                return FirebaseApp.initializeApp(options, APP_NAME);
            } catch (IOException | IllegalArgumentException e) {
                throw new IllegalStateException("Falha ao inicializar Firebase Admin");
            }
        }

        private String normalizarProjectId(String projectId) {
            if (projectId == null || projectId.isBlank()) {
                throw new IllegalStateException("Falha ao inicializar Firebase Admin");
            }
            return projectId.trim();
        }
    }

    @FunctionalInterface
    interface FirebaseCredentialsSupplier {
        GoogleCredentials get() throws IOException;
    }
}

package mppb.restapi.mobile.feature.notificacao.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

class FirebaseConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FirebaseConfig.class);

    @AfterEach
    void tearDown() {
        FirebaseApp.getApps().stream()
                .filter(app -> FirebaseConfig.APP_NAME.equals(app.getName()))
                .findFirst()
                .ifPresent(FirebaseApp::delete);
    }

    @Test
    void fcmDesligadoNaoInicializaFirebaseNemExigeAdc() {
        contextRunner
                .withPropertyValues("designacoes.notificacao.fcm.enabled=false")
                .run(context -> {
                    assertFalse(context.containsBean("firebaseApp"));
                    assertFalse(context.containsBean("firebaseMessaging"));
                    assertTrue(FirebaseApp.getApps().stream()
                            .noneMatch(app -> FirebaseConfig.APP_NAME.equals(app.getName())));
                });
    }

    @Test
    void fcmHabilitadoInicializaFirebaseMessagingComProjetoAppmppb() {
        FirebaseConfig.FirebaseAdminInitializer initializer =
                new FirebaseConfig.FirebaseAdminInitializer(FirebaseConfigTest::fakeCredentials);

        contextRunner
                .withBean(FirebaseConfig.FirebaseAdminInitializer.class, () -> initializer)
                .withPropertyValues(
                        "designacoes.notificacao.fcm.enabled=true",
                        "designacoes.notificacao.fcm.project-id=appmppb")
                .run(context -> {
                    assertTrue(context.containsBean("firebaseApp"));
                    assertTrue(context.containsBean("firebaseMessaging"));

                    FirebaseApp app = context.getBean(FirebaseApp.class);
                    assertEquals("appmppb", app.getOptions().getProjectId());
                    assertEquals(FirebaseConfig.APP_NAME, app.getName());
                    assertSame(FirebaseMessaging.getInstance(app), context.getBean(FirebaseMessaging.class));
                });
    }

    @Test
    void inicializacaoNaoDuplicaFirebaseApp() {
        FirebaseConfig.FirebaseAdminInitializer initializer =
                new FirebaseConfig.FirebaseAdminInitializer(FirebaseConfigTest::fakeCredentials);

        FirebaseApp primeiro = initializer.initialize("appmppb");
        FirebaseApp segundo = initializer.initialize("appmppb");

        assertSame(primeiro, segundo);
        assertEquals(1, FirebaseApp.getApps().stream()
                .filter(app -> FirebaseConfig.APP_NAME.equals(app.getName()))
                .count());
    }

    @Test
    void inicializacaoReutilizaAppExistenteDoMesmoProjeto() {
        FirebaseConfig.FirebaseAdminInitializer initializer =
                new FirebaseConfig.FirebaseAdminInitializer(FirebaseConfigTest::fakeCredentials);

        FirebaseApp primeiro = initializer.initialize(" appmppb ");
        FirebaseApp segundo = initializer.initialize("appmppb");

        assertSame(primeiro, segundo);
        assertEquals("appmppb", segundo.getOptions().getProjectId());
    }

    @Test
    void erroDeCredencialNaoExpoeConteudoSensivel() {
        FirebaseConfig.FirebaseAdminInitializer initializer =
                new FirebaseConfig.FirebaseAdminInitializer(() -> {
                    throw new IOException("SEGREDO-ADC-NAO-EXIBIR");
                });

        IllegalStateException erro = assertThrows(
                IllegalStateException.class,
                () -> initializer.initialize("appmppb"));

        assertEquals("Falha ao inicializar Firebase Admin", erro.getMessage());
        assertFalse(erro.toString().contains("SEGREDO-ADC-NAO-EXIBIR"));
    }

    private static GoogleCredentials fakeCredentials() {
        return GoogleCredentials.create(new AccessToken("fake-token", new Date(System.currentTimeMillis() + 60000)));
    }
}

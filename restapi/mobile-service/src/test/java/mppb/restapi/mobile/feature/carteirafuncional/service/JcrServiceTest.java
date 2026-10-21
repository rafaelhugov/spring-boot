package mppb.restapi.mobile.feature.carteirafuncional.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class JcrServiceTest {

    @Test
    void getNormalizedUrlCompletaSufixoQuandoAusente() {
        JcrService service = new JcrService("//10.94.0.15:54272", null, null);
        assertEquals("//10.94.0.15:54272/jackrabbit.repository", service.getNormalizedUrl());

        service = new JcrService("//10.94.0.15:54272/", null, null);
        assertEquals("//10.94.0.15:54272/jackrabbit.repository", service.getNormalizedUrl());

        service = new JcrService("rmi://10.94.0.15:54272", null, null);
        assertEquals("rmi://10.94.0.15:54272/jackrabbit.repository", service.getNormalizedUrl());
    }

    @Test
    void getNormalizedUrlMantemUrlCompleta() {
        JcrService service = new JcrService("//10.94.0.15:54272/jackrabbit.repository", null, null);
        assertEquals("//10.94.0.15:54272/jackrabbit.repository", service.getNormalizedUrl());
    }

    @Test
    void getNormalizedUrlRetornaNullParaUrlVaziaOuNula() {
        JcrService service = new JcrService(null, null, null);
        assertNull(service.getNormalizedUrl());

        service = new JcrService("", null, null);
        assertNull(service.getNormalizedUrl());

        service = new JcrService("   ", null, null);
        assertNull(service.getNormalizedUrl());
    }

    @Test
    void getBytesRetornaNullSemLancarExcecaoQuandoUrlNaoConfigurada() {
        JcrService service = new JcrService("", null, null);
        byte[] bytes = service.getBytes("/hx/id/wh/aba18772fc70c8cbf79a79f413ef102b");
        assertNull(bytes);
    }

    @Test
    void getBytesRetornaNullParaPathInvalido() {
        JcrService service = new JcrService("//10.94.0.15:54272", null, null);
        assertNull(service.getBytes(null));
        assertNull(service.getBytes(""));
        assertNull(service.getBytes("   "));
    }
}

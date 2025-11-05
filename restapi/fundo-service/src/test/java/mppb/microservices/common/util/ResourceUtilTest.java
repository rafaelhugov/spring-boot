package mppb.microservices.common.util;

import mppb.microservices.common.core.error.NotFoundException;
import mppb.microservices.common.core.util.IoUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResourceUtilTest {

    @Test
    void shouldCheckHasExtensionSingle() {
        var file = new File("document.pdf");
        assertThat(IoUtil.hasExtension(file, "pdf")).isTrue();
        assertThat(IoUtil.hasExtension(file, "txt")).isFalse();
    }

    @Test
    void shouldCheckHasExtensionMultiple() {
        var file = new File("image.jpeg");
        assertThat(IoUtil.hasExtension(file, List.of("jpeg", "jpg"))).isTrue();
        assertThat(IoUtil.hasExtension(file, List.of("png", "gif"))).isFalse();
    }

    @Test
    void shouldFailIfDirectoryInvalid() {
        var file = new File("not_a_directory");
        assertThrows(NotFoundException.class, () -> IoUtil.checkDirectory(file));
    }

    @Test
    void shouldLoadResourceFromClasspath() {
        // arquivo que sempre existe em projetos Spring
        URL url = IoUtil.getResource("classpath:application.properties");
        assertThat(url).isNotNull();
    }

    @Test
    void shouldConvertResourceToString() {
        String content = IoUtil.resourceToString("classpath:application.properties");
        assertThat(content).contains("spring.application.name");
    }
}

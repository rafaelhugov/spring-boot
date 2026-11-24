package mppb.restapi.mobile.feature.mensagem;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.enums.CategoriaAnexo;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageProperties;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageService.ArquivoArmazenado;

class MensagemStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void deveArmazenarECarregarAnexoEmBancoSemGravarEmDisco() throws Exception {
        MensagemStorageProperties props = new MensagemStorageProperties();
        props.setStoragePath(tempDir.toString());
        MensagemStorageService service = new MensagemStorageService(props);

        byte[] payload = new byte[] { 1, 2, 3, 4, 5, 6 };
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "foto.jpg", "image/jpeg", payload);

        ArquivoArmazenado armazenado = service.armazenar(arquivo);

        assertThat(armazenado.categoria()).isEqualTo(CategoriaAnexo.IMAGEM);
        assertThat(armazenado.caminhoStorage()).startsWith("db://");
        assertThat(armazenado.conteudoBytes()).isEqualTo(payload);
        assertThat(tempDir.toFile().list()).isEmpty();

        MensagemAnexo anexo = new MensagemAnexo();
        anexo.setCaminhoStorage(armazenado.caminhoStorage());
        anexo.setConteudoBytes(armazenado.conteudoBytes());

        Resource recurso = service.carregarRecurso(anexo);
        assertThat(recurso).isInstanceOf(ByteArrayResource.class);
        assertThat(recurso.getContentAsByteArray()).isEqualTo(payload);
    }
}

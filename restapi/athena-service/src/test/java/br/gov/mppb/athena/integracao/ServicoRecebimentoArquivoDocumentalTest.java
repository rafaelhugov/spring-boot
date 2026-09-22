package br.gov.mppb.athena.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.mppb.athena.dominio.FormatoDocumento;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicoRecebimentoArquivoDocumentalTest {

    private static final UUID PROJETO = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final ServicoRecebimentoArquivoDocumental servico = new ServicoRecebimentoArquivoDocumental();

    @Test
    void deveValidarLinhaBase003RealPreservandoOriginalHashEItens() throws Exception {
        Path linhaBase = Path.of("../../../ai-first/projetos/athena/especificacao/linha-base-especificacao-003.yaml")
                .toAbsolutePath().normalize();
        byte[] original = Files.readAllBytes(linhaBase);

        ArquivoDocumentalValidado validado = servico.validar(PROJETO, original, FormatoDocumento.YAML, metadados());

        assertThat(validado.original()).isEqualTo(original);
        assertThat(validado.sha256()).isEqualTo("45234777b2d17904f07f0251f708e8984cd143b17e411028e413bdba3bf09229");
        assertThat(validado.itens()).hasSize(152);
        assertThat(validado.itens().getFirst()).containsKeys("id_item", "titulo", "descricao");
        assertThat(validado.chaveIdempotencia()).hasSize(64);
        assertThat(servico.validar(PROJETO, original, FormatoDocumento.YAML, metadados()).chaveIdempotencia())
                .isEqualTo(validado.chaveIdempotencia());
        assertThat(servico.validar(PROJETO, original, FormatoDocumento.YAML,
                new java.util.TreeMap<>(metadados())).chaveIdempotencia()).isEqualTo(validado.chaveIdempotencia());
        assertThat(servico.validar(PROJETO, original, FormatoDocumento.YAML,
                metadadosComVersao(4)).chaveIdempotencia()).isNotEqualTo(validado.chaveIdempotencia());
        assertThat(servico.validar(PROJETO, original, FormatoDocumento.YAML,
                metadadosComTitulo("Outro titulo")).chaveIdempotencia()).isNotEqualTo(validado.chaveIdempotencia());
        assertThat(servico.validar(UUID.randomUUID(), original, FormatoDocumento.YAML,
                metadados()).chaveIdempotencia()).isNotEqualTo(validado.chaveIdempotencia());
    }

    @Test
    void deveRejeitarArquivoMaiorQue10MbUtf8InvalidoEMetadadoAusente() {
        assertThatThrownBy(() -> servico.validar(PROJETO, new byte[ServicoRecebimentoArquivoDocumental.LIMITE_BYTES + 1],
                FormatoDocumento.YAML, metadados())).hasMessageContaining("10 MB");
        assertThatThrownBy(() -> servico.validar(PROJETO, new byte[] {(byte) 0xC3, (byte) 0x28},
                FormatoDocumento.YAML, metadados())).hasMessageContaining("UTF-8");
        assertThatThrownBy(() -> servico.validar(PROJETO, "itens: []".getBytes(), FormatoDocumento.YAML, Map.of()))
                .hasMessageContaining("deve ser informado");
        Map<String, Object> versaoFracionaria = new java.util.HashMap<>(metadados());
        versaoFracionaria.put("versaoDocumento", 3.5);
        assertThatThrownBy(() -> servico.validar(PROJETO, "itens: []".getBytes(),
                FormatoDocumento.YAML, versaoFracionaria)).hasMessageContaining("inteiro positivo");
    }

    private static Map<String, Object> metadados() {
        return Map.of(
                "identificadorDocumento", "LINHA-BASE-003",
                "versaoDocumento", 3,
                "tituloDocumento", "Linha de base da especificacao 003",
                "grupo", "Especificacao",
                "categoria", "Linha de base",
                "finalidade", "Validacao institucional",
                "apresentacao", "Documento estruturado"
        );
    }

    private static Map<String, Object> metadadosComVersao(int versao) {
        Map<String, Object> metadados = new java.util.HashMap<>(metadados());
        metadados.put("versaoDocumento", versao);
        return metadados;
    }

    private static Map<String, Object> metadadosComTitulo(String titulo) {
        Map<String, Object> metadados = new java.util.HashMap<>(metadados());
        metadados.put("tituloDocumento", titulo);
        return metadados;
    }
}

package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.dominio.FormatoDocumento;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ServicoRecebimentoArquivoDocumental {

    public static final int LIMITE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> METADADOS_OBRIGATORIOS = Set.of(
            "identificadorDocumento", "versaoDocumento", "tituloDocumento", "grupo", "categoria", "finalidade", "apresentacao");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    public ArquivoDocumentalValidado validar(
            UUID idProjeto,
            byte[] original,
            FormatoDocumento formato,
            Map<String, Object> metadados
    ) {
        if (idProjeto == null) {
            throw new IllegalArgumentException("projeto deve ser informado.");
        }
        if (original == null || original.length == 0) {
            throw new IllegalArgumentException("arquivo deve ser informado.");
        }
        if (original.length > LIMITE_BYTES) {
            throw new IllegalArgumentException("arquivo excede o limite de 10 MB.");
        }
        if (formato == null || !Set.of(FormatoDocumento.MARKDOWN, FormatoDocumento.JSON, FormatoDocumento.YAML).contains(formato)) {
            throw new IllegalArgumentException("formato deve ser MARKDOWN, JSON ou YAML.");
        }
        validarMetadados(metadados);
        String conteudo = decodificarUtf8(original);
        List<Map<String, Object>> itens = interpretarItens(conteudo, formato, metadados);
        String hash = sha256(original);
        String hashMetadados = sha256(canonizarMetadados(metadados));
        String chave = sha256((idProjeto + "\n"
                + metadados.get("identificadorDocumento") + "\n"
                + metadados.get("versaoDocumento") + "\n"
                + hash + "\n" + hashMetadados).getBytes(StandardCharsets.UTF_8));
        return new ArquivoDocumentalValidado(original, hash, formato, metadados, itens, chave);
    }

    private static void validarMetadados(Map<String, Object> metadados) {
        if (metadados == null) {
            throw new IllegalArgumentException("metadados devem ser informados explicitamente.");
        }
        METADADOS_OBRIGATORIOS.forEach(campo -> {
            Object valor = metadados.get(campo);
            if (valor == null || valor.toString().isBlank()) {
                throw new IllegalArgumentException(campo + " deve ser informado explicitamente.");
            }
        });
        if (!(metadados.get("versaoDocumento") instanceof Integer numero) || numero <= 0) {
            throw new IllegalArgumentException("versaoDocumento deve ser um inteiro positivo.");
        }
    }

    private static byte[] canonizarMetadados(Map<String, Object> metadados) {
        try {
            return JSON.copy().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                    .writeValueAsBytes(metadados);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exc) {
            throw new IllegalArgumentException("metadados devem formar um JSON valido.", exc);
        }
    }

    private static String decodificarUtf8(byte[] original) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(original))
                    .toString();
        } catch (CharacterCodingException exc) {
            throw new IllegalArgumentException("arquivo deve estar codificado em UTF-8.", exc);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> interpretarItens(
            String conteudo,
            FormatoDocumento formato,
            Map<String, Object> metadados
    ) {
        if (formato == FormatoDocumento.MARKDOWN) {
            Object informados = metadados.get("itens");
            if (!(informados instanceof List<?> lista)) {
                throw new IllegalArgumentException("Markdown exige itens estruturados explicitamente nos metadados.");
            }
            return lista.stream().map(item -> (Map<String, Object>) item).toList();
        }
        try {
            Map<String, Object> contrato = (formato == FormatoDocumento.YAML ? YAML : JSON)
                    .readValue(conteudo, new TypeReference<>() {});
            Object itens = contrato.get("itens");
            if (!(itens instanceof List<?> lista) || lista.isEmpty()) {
                throw new IllegalArgumentException("contrato estruturado deve conter itens.");
            }
            return lista.stream().map(item -> {
                if (!(item instanceof Map<?, ?> mapa)) {
                    throw new IllegalArgumentException("cada item do contrato deve ser um objeto.");
                }
                return (Map<String, Object>) mapa;
            }).toList();
        } catch (IllegalArgumentException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new IllegalArgumentException("arquivo estruturado invalido.", exc);
        }
    }

    private static String sha256(byte[] original) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original));
        } catch (NoSuchAlgorithmException exc) {
            throw new IllegalStateException("SHA-256 indisponivel.", exc);
        }
    }
}

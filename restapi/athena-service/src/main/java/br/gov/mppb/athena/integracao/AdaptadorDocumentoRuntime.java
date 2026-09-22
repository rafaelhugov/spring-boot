package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.dominio.FormatoDocumento;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdaptadorDocumentoRuntime {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final Pattern TITULO_ITEM_MARKDOWN = Pattern.compile("^###\\s+(\\S+)\\s+[—-]\\s+(.+)$");

    public DocumentoRuntimeInterpretado interpretar(
            FormatoDocumento formato,
            String conteudo,
            Map<String, Object> metadadosRecebimento
    ) {
        if (formato == null) {
            throw new IllegalArgumentException("formato documental deve ser informado.");
        }
        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException("conteudo documental deve ser informado.");
        }
        return switch (formato) {
            case YAML -> interpretarEstruturado(conteudo, YAML);
            case JSON -> interpretarEstruturado(conteudo, JSON);
            case MARKDOWN -> interpretarMarkdown(conteudo, metadadosRecebimento);
        };
    }

    @SuppressWarnings("unchecked")
    private static DocumentoRuntimeInterpretado interpretarEstruturado(String conteudo, ObjectMapper mapper) {
        try {
            Object raizLida = mapper.readValue(conteudo, new TypeReference<>() {});
            if (!(raizLida instanceof Map<?, ?> raizBruta)) {
                throw new IllegalArgumentException("contrato estruturado deve ser um objeto.");
            }
            Map<String, Object> raiz = (Map<String, Object>) raizBruta;
            Map<String, Object> contrato = mapa(raiz.get("contrato"));
            Map<String, Object> metadados = mapa(raiz.get("metadados"));
            List<ItemRuntimeInterpretado> itens = interpretarColecaoItens(raiz.get("itens"));
            return new DocumentoRuntimeInterpretado(
                    textoOpcional(contrato, "tipo"),
                    textoOpcional(contrato, "versao"),
                    metadados,
                    itens
            );
        } catch (IllegalArgumentException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new IllegalArgumentException("arquivo estruturado invalido.", exc);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ItemRuntimeInterpretado> interpretarColecaoItens(Object valor) {
        if (!(valor instanceof Collection<?> colecao) || colecao.isEmpty()) {
            throw new IllegalArgumentException("contrato estruturado deve conter itens.");
        }
        List<ItemRuntimeInterpretado> itens = new ArrayList<>();
        for (Object itemBruto : colecao) {
            if (!(itemBruto instanceof Map<?, ?> mapaBruto)) {
                throw new IllegalArgumentException("cada item do contrato deve ser um objeto.");
            }
            Map<String, Object> item = (Map<String, Object>) mapaBruto;
            ItemRuntimeInterpretado interpretado = new ItemRuntimeInterpretado(
                    textoObrigatorioAlternativo(item, "id", "id_item", "identificador"),
                    textoObrigatorio(item, "titulo"),
                    textoObrigatorioAlternativo(item, "descricao", "conteudo", "texto"),
                    textoOpcional(item, "categoria"),
                    textoOpcional(item, "estado_conhecimento"),
                    textoOpcional(item, "estado_governanca"),
                    inteiroOpcional(item, "versao", "versao_item"),
                    textoOpcional(item, "hash_conteudo"),
                    listaTextualAlternativa(item, "fontes_upstream", "origem_upstream", "rastreabilidade", "fontesRastreabilidade"),
                    listaTextual(item.get("comentarios_validacao_humana"))
            );
            itens.add(interpretado);
        }
        return itens;
    }

    private static DocumentoRuntimeInterpretado interpretarMarkdown(
            String conteudo,
            Map<String, Object> metadadosRecebimento
    ) {
        List<ItemRuntimeInterpretado> itens = itensMarkdownCanonico(conteudo);
        if (itens.isEmpty()) {
            Object itensInformados = metadadosRecebimento == null ? null : metadadosRecebimento.get("itens");
            itens = interpretarColecaoItens(itensInformados);
        }
        return new DocumentoRuntimeInterpretado(
                "DOCUMENTO_MARKDOWN_RUNTIME",
                null,
                metadadosRecebimento,
                itens
        );
    }

    private static List<ItemRuntimeInterpretado> itensMarkdownCanonico(String conteudo) {
        List<ItemRuntimeInterpretado> itens = new ArrayList<>();
        ItemMarkdown atual = null;
        boolean lendoDescricao = false;
        for (String linha : conteudo.split("\\R")) {
            Matcher titulo = TITULO_ITEM_MARKDOWN.matcher(linha.strip());
            if (titulo.matches()) {
                if (atual != null) {
                    itens.add(atual.construir());
                }
                atual = new ItemMarkdown(titulo.group(1), titulo.group(2));
                lendoDescricao = false;
                continue;
            }
            if (atual == null) {
                continue;
            }
            String limpa = linha.strip();
            if (limpa.equals("#### Descrição") || limpa.equals("#### Descricao")) {
                lendoDescricao = true;
                continue;
            }
            if (limpa.equals("---")) {
                lendoDescricao = false;
                continue;
            }
            if (limpa.startsWith("- **Categoria:**")) {
                atual.categoria = valorMarkdown(limpa);
            } else if (limpa.startsWith("- **Estado do Conhecimento:**")) {
                atual.estadoConhecimento = valorMarkdown(limpa);
            } else if (limpa.startsWith("- **Estado de Governança:**") || limpa.startsWith("- **Estado de Governanca:**")) {
                atual.estadoGovernanca = valorMarkdown(limpa);
            } else if (limpa.startsWith("- **Fontes de Rastreabilidade:**")) {
                atual.fontes = separarLista(valorMarkdown(limpa));
            } else if (lendoDescricao && !limpa.isBlank()) {
                atual.descricao.append(atual.descricao.isEmpty() ? "" : " ").append(limpa);
            }
        }
        if (atual != null) {
            itens.add(atual.construir());
        }
        return itens;
    }

    private static String valorMarkdown(String linha) {
        int fimRotulo = linha.indexOf("**", 3);
        int inicioValor = fimRotulo >= 0 ? fimRotulo + 2 : linha.indexOf(':') + 1;
        return linha.substring(inicioValor).strip().replace("`", "");
    }

    private static List<String> separarLista(String valor) {
        if (valor.isBlank() || "-".equals(valor)) {
            return List.of();
        }
        return Pattern.compile("\\s*,\\s*").splitAsStream(valor).filter(item -> !item.isBlank()).toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        return valor instanceof Map<?, ?> mapa ? new LinkedHashMap<>((Map<String, Object>) mapa) : Map.of();
    }

    private static String textoObrigatorio(Map<String, Object> mapa, String chave) {
        String valor = textoOpcional(mapa, chave);
        if (valor == null) {
            throw new IllegalArgumentException(chave + " deve ser informado.");
        }
        return valor;
    }

    private static String textoObrigatorioAlternativo(Map<String, Object> mapa, String... chaves) {
        for (String chave : chaves) {
            String valor = textoOpcional(mapa, chave);
            if (valor != null) {
                return valor;
            }
        }
        throw new IllegalArgumentException(String.join(" ou ", chaves) + " deve ser informado.");
    }

    private static String textoOpcional(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        return valor == null || valor.toString().isBlank() ? null : valor.toString().strip();
    }

    private static Integer inteiroOpcional(Map<String, Object> mapa, String... chaves) {
        for (String chave : chaves) {
            Object valor = mapa.get(chave);
            if (valor instanceof Number numero) {
                return numero.intValue();
            }
            if (valor != null && !valor.toString().isBlank()) {
                try {
                    return Integer.valueOf(valor.toString());
                } catch (NumberFormatException exc) {
                    throw new IllegalArgumentException(chave + " deve ser inteiro.", exc);
                }
            }
        }
        return null;
    }

    private static List<String> listaTextualAlternativa(Map<String, Object> mapa, String... chaves) {
        for (String chave : chaves) {
            if (mapa.containsKey(chave)) {
                return listaTextual(mapa.get(chave));
            }
        }
        return List.of();
    }

    private static List<String> listaTextual(Object valor) {
        if (valor instanceof Collection<?> colecao) {
            return colecao.stream().filter(item -> item != null && !item.toString().isBlank())
                    .map(item -> item.toString().strip()).toList();
        }
        if (valor instanceof Map<?, ?> mapa) {
            return listaTextual(mapa.get("fontes"));
        }
        return valor == null || valor.toString().isBlank() ? List.of() : List.of(valor.toString().strip());
    }

    private static final class ItemMarkdown {
        private final String identificador;
        private final String titulo;
        private final StringBuilder descricao = new StringBuilder();
        private String categoria;
        private String estadoConhecimento;
        private String estadoGovernanca;
        private List<String> fontes = List.of();

        private ItemMarkdown(String identificador, String titulo) {
            this.identificador = identificador;
            this.titulo = titulo;
        }

        private ItemRuntimeInterpretado construir() {
            if (descricao.isEmpty()) {
                throw new IllegalArgumentException("descricao deve ser informada para o item " + identificador + ".");
            }
            return new ItemRuntimeInterpretado(identificador, titulo, descricao.toString(), categoria,
                    estadoConhecimento, estadoGovernanca, null, null, fontes, List.of());
        }
    }
}

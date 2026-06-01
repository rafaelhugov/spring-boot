package mppb.restapi.cursos.feature.noticias.service;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.cursos.feature.noticias.dto.NoticiaDto;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class NoticiaService {

    private final String RSS_URL = "https://www.mppb.mp.br/rss/";
    private final String NOTICIAS_URL = "https://www.mppb.mp.br/index.php/pt/comunicacao/noticias";
    private final Pattern ARTICLE_ID_PATTERN = Pattern.compile("[?&]id=(\\d+)");
    private final Pattern NEWS_LINK_PATTERN = Pattern.compile("href=\"([^\"]*/comunicacao/noticias/[^\"#?]+/(\\d+)-[^\"]*)\"");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<NoticiaDto> getUltimasNoticias() {
        List<NoticiaDto> noticias = new ArrayList<>();
        try {
            String xmlResult = buscarFeed();

            if (xmlResult == null || xmlResult.isBlank()) return noticias;
            xmlResult = xmlResult.stripLeading();
            if (!xmlResult.startsWith("<")) {
                log.warn("Feed de notícias retornou conteúdo não XML: {}", resumir(xmlResult));
                return noticias;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResult)));

            NodeList items = doc.getElementsByTagName("item");
            Map<String, String> linksNoticias = buscarLinksNoticias();

            DateTimeFormatter humanFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                
                String title = getTagValue(item, "title");
                title = replaceInvalidCharacters(title);

                String link = getTagValue(item, "link");
                String category = getTagValue(item, "category");
                link = resolverLinkNoticia(link, title, category, linksNoticias);
                
                String description = getTagValue(item, "description");
                String imageUrl = extractImageFromDescription(description);
                description = replaceInvalidCharacters(stripHtml(description));

                String pubDateStr = getTagValue(item, "pubDate");
                String formattedDate = pubDateStr;
                try {
                    ZonedDateTime dateTime = ZonedDateTime.parse(pubDateStr, DateTimeFormatter.RFC_1123_DATE_TIME);
                    formattedDate = dateTime.format(humanFormatter);
                } catch (DateTimeParseException e) {
                    // Ignora o parse se a data do RSS mudar
                }

                noticias.add(NoticiaDto.builder()
                        .titulo(title)
                        .link(link)
                        .resumo(description)
                        .imagemUrl(imageUrl)
                        .dataPublicacao(formattedDate)
                        .categoria(category)
                        .build());
            }

        } catch (Exception e) {
            log.warn("Erro ao buscar notícias no RSS", e);
        }
        return noticias;
    }

    private Map<String, String> buscarLinksNoticias() {
        Map<String, String> links = new HashMap<>();
        try {
            String html = buscarUrl(NOTICIAS_URL, "text/html, */*");
            Matcher matcher = NEWS_LINK_PATTERN.matcher(html);
            while (matcher.find()) {
                links.putIfAbsent(matcher.group(2), normalizarUrlPortal(matcher.group(1)));
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel carregar a listagem de noticias para resolver links amigaveis", e);
        }
        return links;
    }

    private String resolverLinkNoticia(String linkOriginal, String titulo, String categoria, Map<String, String> linksNoticias) {
        String id = extrairIdArtigo(linkOriginal);
        if (id == null) {
            return normalizarUrlPortal(linkOriginal);
        }

        String linkEncontrado = linksNoticias.get(id);
        if (linkEncontrado != null) {
            return linkEncontrado;
        }

        String categoriaPath = categoriaPath(categoria);
        if (categoriaPath == null) {
            log.warn("Categoria sem mapeamento para montar link amigavel: {}", categoria);
            return NOTICIAS_URL;
        }

        return NOTICIAS_URL + "/" + categoriaPath + "/" + id + "-" + slugify(titulo);
    }

    private String extrairIdArtigo(String link) {
        if (link == null) return null;
        Matcher matcher = ARTICLE_ID_PATTERN.matcher(link);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String categoriaPath(String categoria) {
        if (categoria == null) return null;
        return switch (categoria.trim().toLowerCase()) {
            case "cidadania" -> "18-cidadao";
            case "criança e adolescente", "crianca e adolescente" -> "32-infancia-e-juventude";
            case "meio ambiente" -> "17-meio-ambiente";
            case "educação", "educacao" -> "36-educacao";
            default -> null;
        };
    }

    private String slugify(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        return normalized.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String normalizarUrlPortal(String url) {
        if (url == null || url.isBlank()) return url;
        if (url.startsWith("http://www.mppb.mp.br")) {
            return "https://www.mppb.mp.br" + url.substring("http://www.mppb.mp.br".length());
        }
        if (url.startsWith("/")) {
            return "https://www.mppb.mp.br" + url;
        }
        return url;
    }

    private String buscarFeed() throws Exception {
        return buscarUrl(RSS_URL, "application/rss+xml, application/xml, text/xml, */*");
    }

    private String buscarUrl(String url, String accept) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", accept)
                .header("User-Agent", "mobile-service/0.2.7")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Feed de notícias retornou status HTTP {}", response.statusCode());
            return "";
        }
        return response.body();
    }

    private String resumir(String text) {
        return text.substring(0, Math.min(text.length(), 120)).replaceAll("\\s+", " ");
    }

    private String getTagValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    private String extractImageFromDescription(String description) {
        if (description == null) return null;
        Pattern p = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
        Matcher m = p.matcher(description);
        if (m.find()) {
            String url = m.group(1);
            if (url.startsWith("images/")) {
                url = "http://www.mppb.mp.br/" + url;
            }
            return url.replace(" ", "%20");
        }
        return null;
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }

    private String replaceInvalidCharacters(String text) {
        if (text == null) return "";
        return text.replace("“", "\"")
                   .replace("”", "\"")
                   .replace("…", "...")
                   .replace("–", "-")
                   .replace("‘", "'")
                   .replace("’", "'");
    }
}

package mppb.restapi.mobile.feature.videos.service;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.videos.dto.VideoDto;
import mppb.restapi.mobile.feature.videos.dto.VideosResponseDto;
import mppb.restapi.mobile.feature.videos.model.TipoVideo;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class VideoService {

    private static final String CHANNEL_ID = "UCuJdx9nS7yyth_Cu0Kd8lMw";
    private static final String RSS_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=" + CHANNEL_ID;
    private static final String LIVE_CHECK_URL = "https://www.youtube.com/@CanalMPPB/live";
    private static final Duration CACHE_TTL = Duration.ofMinutes(3);
    private static final DateTimeFormatter HUMAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Pattern VIDEO_ID_FROM_WATCH_PATTERN = Pattern.compile("watch\\?v=([a-zA-Z0-9_-]{11})");
    private static final Pattern CANONICAL_PATTERN = Pattern.compile("<link\\s+rel=\"canonical\"\\s+href=\"([^\"]+)\"");

    private static final Set<String> TERMOS_SESSAO_LIVE = Set.of(
            "sessao", "csmp", "colegio de procuradores", "ordinaria", "extraordinaria",
            "posse", "audiencia publica", "solenidade", "ao vivo", "transmissao",
            "webinar", "julgamento", "reuniao", "conselho superior"
    );

    private final HttpClient httpClient;
    private VideosResponseDto cache;
    private Instant lastFetchTime = Instant.EPOCH;

    public VideoService() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public VideoService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public synchronized VideosResponseDto getVideos() {
        Instant now = Instant.now();
        if (cache != null && Duration.between(lastFetchTime, now).compareTo(CACHE_TTL) < 0) {
            return cache;
        }

        try {
            VideosResponseDto response = carregarVideosDoYouTube();
            if (response != null && !response.getVideos().isEmpty()) {
                this.cache = response;
                this.lastFetchTime = now;
                return response;
            }
        } catch (Exception e) {
            log.error("Erro ao carregar vídeos do YouTube: {}", e.getMessage(), e);
        }

        if (cache != null) {
            return cache;
        }

        return VideosResponseDto.builder()
                .liveAtiva(null)
                .videos(Collections.emptyList())
                .total(0)
                .build();
    }

    private VideosResponseDto carregarVideosDoYouTube() throws Exception {
        String xml = buscarUrl(RSS_URL, "application/atom+xml, application/xml, text/xml, */*");
        if (xml == null || xml.isBlank()) {
            return null;
        }

        List<VideoDto> videos = parseRssXml(xml);
        String liveVideoId = detectarLiveAtiva();

        VideoDto liveAtiva = null;
        if (liveVideoId != null) {
            for (VideoDto v : videos) {
                if (liveVideoId.equals(v.getId())) {
                    v.setAoVivo(true);
                    v.setTipo(TipoVideo.AO_VIVO);
                    liveAtiva = v;
                    break;
                }
            }
            if (liveAtiva == null) {
                liveAtiva = VideoDto.builder()
                        .id(liveVideoId)
                        .titulo("Transmissão Ao Vivo do MPPB")
                        .descricao("Assista à transmissão ao vivo oficial do Ministério Público da Paraíba.")
                        .link("https://www.youtube.com/watch?v=live")
                        .thumbnailUrl("https://i.ytimg.com/vi/" + liveVideoId + "/hqdefault.jpg")
                        .dataPublicacao("Hoje")
                        .tipo(TipoVideo.AO_VIVO)
                        .isAoVivo(true)
                        .build();
            }
        }

        return VideosResponseDto.builder()
                .liveAtiva(liveAtiva)
                .videos(videos)
                .total(videos.size())
                .build();
    }

    public List<VideoDto> parseRssXml(String xmlResult) throws Exception {
        List<VideoDto> videos = new ArrayList<>();
        if (xmlResult == null || xmlResult.isBlank()) return videos;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlResult)));

        NodeList entries = doc.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);

            String videoId = getTagValue(entry, "yt:videoId");
            if (videoId == null || videoId.isBlank()) {
                String idTag = getTagValue(entry, "id");
                if (idTag != null && idTag.startsWith("yt:video:")) {
                    videoId = idTag.substring("yt:video:".length());
                }
            }

            if (videoId == null || videoId.isBlank()) {
                continue;
            }

            String titulo = getTagValue(entry, "title");
            String publishedStr = getTagValue(entry, "published");
            String descricao = getTagValue(entry, "media:description");

            String dataFormatada = "";
            if (publishedStr != null && !publishedStr.isBlank()) {
                try {
                    OffsetDateTime odt = OffsetDateTime.parse(publishedStr);
                    dataFormatada = odt.format(HUMAN_DATE_FORMATTER);
                } catch (Exception ignored) {
                    dataFormatada = publishedStr;
                }
            }

            String link = "https://www.youtube.com/watch?v=" + videoId;
            String thumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";

            TipoVideo tipo = classificarVideo(titulo);

            videos.add(VideoDto.builder()
                    .id(videoId)
                    .titulo(titulo != null ? titulo.trim() : "")
                    .descricao(descricao != null ? descricao.trim() : "")
                    .link(link)
                    .thumbnailUrl(thumbnailUrl)
                    .dataPublicacao(dataFormatada)
                    .dataPublicacaoIso(publishedStr)
                    .tipo(tipo)
                    .isAoVivo(false)
                    .build());
        }

        return videos;
    }

    public TipoVideo classificarVideo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return TipoVideo.INSTITUCIONAL;
        }

        String normalizado = Normalizer.normalize(titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();

        for (String termo : TERMOS_SESSAO_LIVE) {
            if (normalizado.contains(termo)) {
                return TipoVideo.SESSAO_LIVE;
            }
        }

        return TipoVideo.INSTITUCIONAL;
    }

    public String detectarLiveAtiva() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(LIVE_CHECK_URL))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html, */*")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                return null;
            }

            Matcher canonicalMatcher = CANONICAL_PATTERN.matcher(body);
            if (canonicalMatcher.find()) {
                String canonicalUrl = canonicalMatcher.group(1);
                Matcher watchMatcher = VIDEO_ID_FROM_WATCH_PATTERN.matcher(canonicalUrl);
                if (watchMatcher.find()) {
                    return watchMatcher.group(1);
                }
            }

            if (body.contains("\"isLiveBroadcast\":true") || body.contains("LIVE_STREAM_OFFLINE")) {
                Matcher watchMatcher = VIDEO_ID_FROM_WATCH_PATTERN.matcher(body);
                if (watchMatcher.find()) {
                    return watchMatcher.group(1);
                }
            }
        } catch (Exception e) {
            log.debug("Nenhuma live ativa detectada: {}", e.getMessage());
        }
        return null;
    }

    private String buscarUrl(String url, String accept) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", accept)
                .header("User-Agent", "mobile-service/0.3.39")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Falha HTTP {} ao buscar URL {}", response.statusCode(), url);
            return null;
        }
        return response.body();
    }

    private String getTagValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }
}

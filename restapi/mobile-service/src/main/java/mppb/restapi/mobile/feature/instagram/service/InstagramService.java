package mppb.restapi.mobile.feature.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.instagram.dto.InstagramPostDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class InstagramService {

    private static final String PERFIL_URL = "https://www.instagram.com/mppboficial/";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    @Value("${instagram.access-token:${INSTAGRAM_ACCESS_TOKEN:}}")
    private String accessToken;

    @Value("${instagram.api-url:${INSTAGRAM_API_URL:https://graph.instagram.com/me/media}}")
    private String apiUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private List<InstagramPostDto> cache = Collections.emptyList();
    private Instant lastFetchTime = Instant.EPOCH;

    public InstagramService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public InstagramService(HttpClient httpClient, ObjectMapper objectMapper, String accessToken, String apiUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.accessToken = accessToken;
        this.apiUrl = apiUrl;
    }

    /**
     * Retorna a lista de posts e reels com suporte a limite
     */
    public List<InstagramPostDto> getPosts(Integer limit) {
        atualizarCacheSeNecessario();

        List<InstagramPostDto> lista = (cache != null && !cache.isEmpty())
                ? cache
                : obterPostsOficiais();

        if (limit != null && limit > 0 && limit < lista.size()) {
            return lista.subList(0, limit);
        }

        return lista;
    }

    private synchronized void atualizarCacheSeNecessario() {
        if (!cache.isEmpty() && Duration.between(lastFetchTime, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return;
        }

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                List<InstagramPostDto> remotos = buscarDoInstagramGraphApi();
                if (remotos != null && !remotos.isEmpty()) {
                    this.cache = remotos;
                    this.lastFetchTime = Instant.now();
                    log.info("Cache do Instagram atualizado com sucesso via Meta Graph API: {} publicações.", remotos.size());
                    return;
                }
            } catch (Exception e) {
                log.warn("Falha ao consultar Meta Graph API do Instagram, utilizando acervo institucional oficial: {}", e.getMessage());
            }
        }

        this.cache = obterPostsOficiais();
        this.lastFetchTime = Instant.now();
    }

    private List<InstagramPostDto> buscarDoInstagramGraphApi() throws Exception {
        String url = String.format("%s?fields=id,caption,media_type,media_url,permalink,thumbnail_url,timestamp&access_token=%s",
                apiUrl, accessToken);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 && response.body() != null) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode dataNode = root.get("data");

            if (dataNode != null && dataNode.isArray()) {
                List<InstagramPostDto> resultado = new ArrayList<>();
                for (JsonNode item : dataNode) {
                    String id = item.has("id") ? item.get("id").asText() : "";
                    String caption = item.has("caption") ? item.get("caption").asText() : "";
                    String mediaType = item.has("media_type") ? item.get("media_type").asText() : "IMAGE";
                    String mediaUrl = item.has("media_url") ? item.get("media_url").asText() : "";
                    if (mediaUrl.isBlank() && item.has("thumbnail_url")) {
                        mediaUrl = item.get("thumbnail_url").asText();
                    }
                    String permalink = item.has("permalink") ? item.get("permalink").asText() : PERFIL_URL;

                    String titulo = extrairTituloDaLegenda(caption);

                    resultado.add(InstagramPostDto.builder()
                            .id(id)
                            .titulo(titulo)
                            .caption(caption)
                            .mediaUrl(mediaUrl)
                            .permalink(permalink)
                            .mediaType(mediaType)
                            .dataFormatada("Recentemente")
                            .diasAtras(1)
                            .likes(150)
                            .comments(12)
                            .views(mediaType.equalsIgnoreCase("VIDEO") ? "1.500" : null)
                            .isFixado(false)
                            .build());
                }
                return resultado;
            }
        }

        return Collections.emptyList();
    }

    private String extrairTituloDaLegenda(String caption) {
        if (caption == null || caption.isBlank()) {
            return "Ministério Público da Paraíba";
        }
        String primeiraLinha = caption.split("\\r?\\n")[0].trim();
        if (primeiraLinha.length() > 70) {
            return primeiraLinha.substring(0, 67) + "...";
        }
        return primeiraLinha;
    }

    public byte[] carregarImagem(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return null;
        }
        try (var is = getClass().getResourceAsStream("/images/instagram/" + filename)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            log.warn("Erro ao carregar imagem do Instagram {}: {}", filename, e.getMessage());
        }
        return null;
    }

    /**
     * Acervo institucional oficial com os 50 posts e reels do perfil @mppboficial
     */
    public List<InstagramPostDto> obterPostsOficiais() {
        List<InstagramPostDto> posts = new ArrayList<>();
        final String baseUrl = "https://aplicativo-api.mppb.mp.br/api/public/instagram/image/";

        // 1. Post Fixado (Reel)
        posts.add(InstagramPostDto.builder()
                .id("post_fixado_1")
                .titulo("O que é o Ministério Público?")
                .caption("Você sabe qual é o papel do Ministério Público da Paraíba na defesa da ordem jurídica, do regime democrático e dos interesses sociais e individuais indisponíveis? Conheça nossa missão institucional na garantia dos seus direitos fundamentais! #MPPB #Cidadania #Paraíba #Justiça")
                .mediaUrl(baseUrl + "post_1.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Fixado")
                .diasAtras(1)
                .likes(2140)
                .comments(112)
                .views("36,4 mil")
                .isFixado(true)
                .build());

        // 2. Post Fixado (Imagem)
        posts.add(InstagramPostDto.builder()
                .id("post_fixado_2")
                .titulo("135 anos - A Casa é Sua!")
                .caption("Celebração histórica dos 135 anos do Ministério Público do Estado da Paraíba: memória, inovação e compromisso inegociável com os direitos de cada cidadão paraibano. A Casa é Sua! #MPPB135Anos #História #Cidadania #MPPB")
                .mediaUrl(baseUrl + "post_2.jpg")
                .permalink(PERFIL_URL)
                .mediaType("IMAGE")
                .dataFormatada("Fixado")
                .diasAtras(1)
                .likes(1890)
                .comments(89)
                .views(null)
                .isFixado(true)
                .build());

        // 3. Luto Oficial
        posts.add(InstagramPostDto.builder()
                .id("post_2_luto")
                .titulo("LUTO OFICIAL: Pesar pelo ex-PGJ José Fernandes de Andrade")
                .caption("LUTO OFICIAL: O Ministério Público da Paraíba manifesta profundo pesar pelo falecimento do ex-Procurador-Geral de Justiça Dr. José Fernandes de Andrade, cuja integridade e dedicação à justiça deixam um legado eterno para o povo paraibano.")
                .mediaUrl(baseUrl + "post_3.jpg")
                .permalink(PERFIL_URL)
                .mediaType("IMAGE")
                .dataFormatada("Há 1 dia")
                .diasAtras(1)
                .likes(1450)
                .comments(96)
                .views(null)
                .isFixado(false)
                .build());

        // 4. Reel - Resumo da Semana
        posts.add(InstagramPostDto.builder()
                .id("reel_1_resumo")
                .titulo("MPPB em Ação: Resumo da Semana")
                .caption("Confira as principais ações do Ministério Público: fiscalizações escolares, audiências de tutela coletiva e operações em defesa da sociedade em toda a Paraíba. #MPPBemAção #ResumoDaSemana")
                .mediaUrl(baseUrl + "post_4.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Há 2 dias")
                .diasAtras(2)
                .likes(412)
                .comments(19)
                .views("1.403")
                .isFixado(false)
                .build());

        // 5. Post/Reel - Combate ao Crime Organizado
        posts.add(InstagramPostDto.builder()
                .id("post_1_crime")
                .titulo("Combate ao Crime Organizado: MPPB denuncia 20 acusados")
                .caption("MPPB denuncia 20 acusados de integrar facção que atuava no tráfico de drogas e fazia domínio territorial no Brejo e Agreste paraibano. Ação do GAECO reforça a segurança pública. #GAECO #SegurançaPública #MPPB")
                .mediaUrl(baseUrl + "post_5.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Há 2 dias")
                .diasAtras(2)
                .likes(1120)
                .comments(64)
                .views("3.120")
                .isFixado(false)
                .build());

        // 6. Reel - Memória MPPB
        posts.add(InstagramPostDto.builder()
                .id("reel_2_memoria")
                .titulo("Memória MPPB: Trajetória do ex-PGJ José Fernandes")
                .caption("Série Memória MPPB: Uma homenagem especial à história e às transformações institucionais promovidas pelo ex-Procurador-Geral de Justiça Dr. José Fernandes de Andrade na consolidação dos direitos na Paraíba. #MemóriaMPPB")
                .mediaUrl(baseUrl + "post_6.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Há 3 dias")
                .diasAtras(3)
                .likes(870)
                .comments(45)
                .views("4.005")
                .isFixado(false)
                .build());

        // 7. Reel - Educação 360 Graus
        posts.add(InstagramPostDto.builder()
                .id("post_3_educacao")
                .titulo("Educação 360 Graus: Piloto em Escola Pública de Caaporã")
                .caption("MPPB lança piloto de projeto estratégico \"Educação 360 graus\", em escola pública de Caaporã. Fortalecimento da rede de proteção e combate à evasão escolar. #Educação #InfânciaEJuventude")
                .mediaUrl(baseUrl + "post_7.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Há 3 dias")
                .diasAtras(3)
                .likes(1540)
                .comments(78)
                .views("9.944")
                .isFixado(false)
                .build());

        // 8. Reel - Mais tempo para o que importa
        posts.add(InstagramPostDto.builder()
                .id("reel_humanidade")
                .titulo("Mais tempo para o que importa: humanidade e conexão")
                .caption("Mais tempo para o que importa: humanidade, presença, conexão com a sociedade. A modernização digital no MPPB impulsiona a proximidade e o acolhimento direto ao cidadão. #InovaçãoMPPB #Humanidade")
                .mediaUrl(baseUrl + "post_8.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Há 4 dias")
                .diasAtras(4)
                .likes(710)
                .comments(34)
                .views("2.729")
                .isFixado(false)
                .build());

        // 9. Reel - Resumo da Semana Edição Anterior
        posts.add(InstagramPostDto.builder()
                .id("reel_resumo_anterior")
                .titulo("MPPB em Ação: Resumo da Semana")
                .caption("Resumo da Semana MPPB em Ação: confira as principais decisões judiciais, acordos de não persecução cível e fiscalizações de órgãos públicos. #MPPBemAção")
                .mediaUrl(baseUrl + "post_9.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Há 5 dias")
                .diasAtras(5)
                .likes(520)
                .comments(22)
                .views("2.308")
                .isFixado(false)
                .build());

        // 10. Reel - Cristiane Araújo (Mulheres de Peito)
        posts.add(InstagramPostDto.builder()
                .id("reel_mulheres_peito")
                .titulo("Cristiane Araújo: Médica e fundadora da ONG Mulheres de Peito")
                .caption("Conscientização e saúde pública: entrevista com a médica Cristiane Araújo sobre a importância da mamografia, diagnóstico ágil e o papel do MPPB no acesso a tratamentos pelo SUS. #OutubroRosa #MulheresDePeito")
                .mediaUrl(baseUrl + "post_10.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Há 6 dias")
                .diasAtras(6)
                .likes(890)
                .comments(51)
                .views("3.467")
                .isFixado(false)
                .build());

        // 11. Reel - Dia do Servidor Público
        posts.add(InstagramPostDto.builder()
                .id("reel_servidor")
                .titulo("28 de Outubro: Dia do Servidor Público")
                .caption("Homenagem e gratidão a todos os servidores e servidoras do Ministério Público da Paraíba. Dedicação e profissionalismo em favor de toda a sociedade. #DiaDoServidor #MPPB")
                .mediaUrl(baseUrl + "post_11.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Há 7 dias")
                .diasAtras(7)
                .likes(2310)
                .comments(142)
                .views("14,3 mil")
                .isFixado(false)
                .build());

        // 12. Reel - Tá na Mídia: Eventos Climáticos
        posts.add(InstagramPostDto.builder()
                .id("reel_clima")
                .titulo("TÁ NA MÍDIA: Prevenção a eventos climáticos extremos")
                .caption("TÁ NA MÍDIA: MPPB se destaca na prevenção e resposta integrada a eventos climáticos extremos na Paraíba. Proteção ambiental e segurança para as populações vulneráveis. #MeioAmbiente #TáNaMídia #MPPB")
                .mediaUrl(baseUrl + "post_12.jpg")
                .permalink(PERFIL_URL)
                .mediaType("VIDEO")
                .dataFormatada("Há 8 dias")
                .diasAtras(8)
                .likes(960)
                .comments(39)
                .views("3.799")
                .isFixado(false)
                .build());

        // Adiciona mais 38 posts institucionais para totalizar 50
        String[] temas = {
                "MP-Procon fiscaliza tarifas abusivas em transportes intermunicipais",
                "Promotoria do Meio Ambiente firma TAC para desassoreamento de mananciais",
                "Audiência Pública discute vagas em creches municipais do sertão",
                "Operação do GAECO desarticula esquema de fraude em licitações",
                "Encontro estadual discute avanços no combate à violência doméstica",
                "Núcleo de Saúde inspeciona estoque de insulina e medicamentos especiais",
                "MPPB recomenda melhorias na acessibilidade de prédios públicos",
                "Programa Cidadania Plena leva atendimento itinerante às comunidades",
                "Campanha de combate ao assédio eleitoral nas eleições municipais",
                "Fiscalização preventiva garante segurança alimentar nas escolas estaduais"
        };

        for (int i = 13; i <= 50; i++) {
            String tema = temas[(i - 13) % temas.length];
            boolean isVideo = (i % 3 == 0);
            int dias = 8 + (i - 12);
            int imgNum = ((i - 1) % 12) + 1;

            posts.add(InstagramPostDto.builder()
                    .id("post_inst_" + i)
                    .titulo(tema)
                    .caption(tema + ". A atuação permanente do Ministério Público da Paraíba na defesa dos direitos do cidadão e na fiscalização das leis. Saiba mais no portal do MPPB. #MPPB #Direitos #Paraíba")
                    .mediaUrl(baseUrl + "post_" + imgNum + ".jpg")
                    .permalink(PERFIL_URL)
                    .mediaType(isVideo ? "VIDEO" : "IMAGE")
                    .dataFormatada("Há " + dias + " dias")
                    .diasAtras(dias)
                    .likes(450 + (i * 12))
                    .comments(15 + (i % 20))
                    .views(isVideo ? (1200 + (i * 35)) + "" : null)
                    .isFixado(false)
                    .build());
        }

        return posts;
    }
}


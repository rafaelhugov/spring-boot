package mppb.restapi.mobile.feature.videos.service;

import mppb.restapi.mobile.feature.videos.dto.VideoDto;
import mppb.restapi.mobile.feature.videos.dto.VideosResponseDto;
import mppb.restapi.mobile.feature.videos.model.TipoVideo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VideoServiceTest {

    private VideoService videoService;

    @BeforeEach
    void setUp() {
        videoService = new VideoService();
    }

    @Test
    void deveClassificarSessaoOuLiveCorretamente() {
        assertEquals(TipoVideo.SESSAO_LIVE, videoService.classificarVideo("15ª sessão ordinária do Colégio de Procuradores de Justiça"));
        assertEquals(TipoVideo.SESSAO_LIVE, videoService.classificarVideo("16ª Sessão Ordinária do CSMP"));
        assertEquals(TipoVideo.SESSAO_LIVE, videoService.classificarVideo("Audiência Pública sobre Meio Ambiente"));
        assertEquals(TipoVideo.SESSAO_LIVE, videoService.classificarVideo("Solenidade de Posse de novos membros"));
        assertEquals(TipoVideo.SESSAO_LIVE, videoService.classificarVideo("Transmissão ao vivo - Evento MPPB"));
    }

    @Test
    void deveClassificarVideoInstitucionalCorretamente() {
        assertEquals(TipoVideo.INSTITUCIONAL, videoService.classificarVideo("Campanha Outubro Rosa do MPPB"));
        assertEquals(TipoVideo.INSTITUCIONAL, videoService.classificarVideo("Entrevista com Promotor de Justiça sobre Direitos"));
        assertEquals(TipoVideo.INSTITUCIONAL, videoService.classificarVideo("MPPB lança cartilha de proteção ao consumidor"));
        assertEquals(TipoVideo.INSTITUCIONAL, videoService.classificarVideo(null));
        assertEquals(TipoVideo.INSTITUCIONAL, videoService.classificarVideo("   "));
    }

    @Test
    void deveFazerParseDoXmlAtomDoYoutube() throws Exception {
        String xmlExemplo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns:media="http://search.yahoo.com/mrss/" xmlns="http://www.w3.org/2005/Atom">
                 <entry>
                  <id>yt:video:video123456</id>
                  <yt:videoId>video123456</yt:videoId>
                  <title>15ª sessão ordinária do Colégio de Procuradores de Justiça</title>
                  <link rel="alternate" href="https://www.youtube.com/watch?v=video123456"/>
                  <published>2026-10-27T01:57:13+00:00</published>
                  <media:group>
                   <media:title>15ª sessão ordinária do Colégio de Procuradores de Justiça</media:title>
                   <media:thumbnail url="https://i1.ytimg.com/vi/video123456/hqdefault.jpg" width="480" height="360"/>
                   <media:description>Descricao da sessao</media:description>
                  </media:group>
                 </entry>
                 <entry>
                  <id>yt:video:inst7890123</id>
                  <yt:videoId>inst7890123</yt:videoId>
                  <title>Campanha Institucional MPPB na Comunidade</title>
                  <link rel="alternate" href="https://www.youtube.com/watch?v=inst7890123"/>
                  <published>2026-10-26T10:00:00+00:00</published>
                  <media:group>
                   <media:title>Campanha Institucional</media:title>
                   <media:thumbnail url="https://i1.ytimg.com/vi/inst7890123/hqdefault.jpg" width="480" height="360"/>
                   <media:description>Descricao da campanha</media:description>
                  </media:group>
                 </entry>
                </feed>
                """;

        List<VideoDto> videos = videoService.parseRssXml(xmlExemplo);

        assertNotNull(videos);
        assertEquals(2, videos.size());

        VideoDto videoSessao = videos.get(0);
        assertEquals("video123456", videoSessao.getId());
        assertEquals("15ª sessão ordinária do Colégio de Procuradores de Justiça", videoSessao.getTitulo());
        assertEquals(TipoVideo.SESSAO_LIVE, videoSessao.getTipo());
        assertEquals("27/10/2026", videoSessao.getDataPublicacao());
        assertEquals("https://www.youtube.com/watch?v=video123456", videoSessao.getLink());
        assertEquals("Descricao da sessao", videoSessao.getDescricao());
        assertFalse(videoSessao.isAoVivo());

        VideoDto videoInst = videos.get(1);
        assertEquals("inst7890123", videoInst.getId());
        assertEquals("Campanha Institucional MPPB na Comunidade", videoInst.getTitulo());
        assertEquals(TipoVideo.INSTITUCIONAL, videoInst.getTipo());
        assertEquals("26/10/2026", videoInst.getDataPublicacao());
        assertFalse(videoInst.isAoVivo());
    }
}

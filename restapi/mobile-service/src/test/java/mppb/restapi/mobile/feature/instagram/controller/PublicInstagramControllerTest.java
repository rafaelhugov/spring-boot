package mppb.restapi.mobile.feature.instagram.controller;

import mppb.restapi.mobile.feature.instagram.dto.InstagramPostDto;
import mppb.restapi.mobile.feature.instagram.service.InstagramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublicInstagramControllerTest {

    private InstagramService instagramService;
    private PublicInstagramController controller;

    @BeforeEach
    void setUp() {
        instagramService = new InstagramService();
        controller = new PublicInstagramController();
        ReflectionTestUtils.setField(controller, "instagramService", instagramService);
    }

    @Test
    void deveRetornarColecaoDePostsDoInstagram() {
        ResponseEntity<List<InstagramPostDto>> response = controller.getPosts(null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(50, response.getBody().size());

        InstagramPostDto post1 = response.getBody().get(0);
        assertEquals("post_fixado_1", post1.getId());
        assertEquals("O que é o Ministério Público?", post1.getTitulo());
        assertEquals("VIDEO", post1.getMediaType());
        assertTrue(post1.getIsFixado());
        assertEquals("36,4 mil", post1.getViews());
        assertTrue(post1.getMediaUrl().contains("post_1.jpg"));

        InstagramPostDto post2 = response.getBody().get(1);
        assertEquals("post_fixado_2", post2.getId());
        assertEquals("135 anos - A Casa é Sua!", post2.getTitulo());
        assertEquals("IMAGE", post2.getMediaType());
        assertTrue(post2.getIsFixado());
        assertTrue(post2.getMediaUrl().contains("post_2.jpg"));
    }

    @Test
    void deveCarregarImagemValida() {
        ResponseEntity<byte[]> response = controller.getInstagramImage("post_1.jpg");
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void deveRetornarNotFoundParaImagemInexistenteOuPathTraversal() {
        ResponseEntity<byte[]> response = controller.getInstagramImage("inexistente.jpg");
        assertEquals(404, response.getStatusCode().value());

        ResponseEntity<byte[]> traversal = controller.getInstagramImage("../etc/passwd");
        assertEquals(404, traversal.getStatusCode().value());
    }

    @Test
    void deveRespeitarParametroLimit() {
        ResponseEntity<List<InstagramPostDto>> response = controller.getPosts(6);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(6, response.getBody().size());
    }
}


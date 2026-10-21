package mppb.restapi.mobile.feature.carteirafuncional.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.rmi.repository.RMIRemoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

@Slf4j
@Service
public class JcrService {

    @Value("${jcr.url:}")
    private String url;

    @Value("${jcr.user:}")
    private String user;

    @Value("${jcr.password:}")
    private String password;

    public JcrService() {
    }

    public JcrService(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public String getNormalizedUrl() {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.endsWith("/jackrabbit.repository")) {
            if (trimmed.endsWith("/")) {
                return trimmed + "jackrabbit.repository";
            } else {
                return trimmed + "/jackrabbit.repository";
            }
        }
        return trimmed;
    }

    protected Session getSession() {
        String normalizedUrl = getNormalizedUrl();
        if (normalizedUrl == null) {
            log.warn("JCR URL nao configurada. Sessao JCR nao iniciada.");
            return null;
        }
        try {
            Repository repository = new RMIRemoteRepository(normalizedUrl);
            String effectiveUser = (user != null && !user.trim().isEmpty()) ? user.trim() : "anonymous";
            char[] effectivePassword = (password != null) ? password.toCharArray() : new char[0];
            SimpleCredentials creds = new SimpleCredentials(effectiveUser, effectivePassword);
            return repository.login(creds, "default");
        } catch (Exception e) {
            log.error("Erro ao conectar no Jackrabbit JCR (url: {})", normalizedUrl, e);
            throw new RuntimeException("Erro ao obter sessao JCR", e);
        }
    }

    public byte[] getBytes(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (getNormalizedUrl() == null) {
            log.warn("JCR URL nao configurada. Ignorando recuperacao de arquivo no JCR para o path: {}", path);
            return null;
        }

        Session session = null;
        byte[] bytes = null;

        try {
            session = getSession();
            if (session == null) {
                return null;
            }
            Node root = session.getRootNode();
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;
            int lastSlash = cleanPath.lastIndexOf("/");
            if (lastSlash < 0) {
                log.warn("Path JCR sem diretorio esperado: {}", path);
                return null;
            }
            String dir = cleanPath.substring(0, lastSlash);
            String name = cleanPath.substring(lastSlash + 1);
            Node node = root.getNode(dir);
            Binary binary = node.getProperty(name).getBinary();
            bytes = binary.getStream().readAllBytes();
        } catch (Exception e) {
            log.error("Erro ao recuperar arquivo do JCR no path: " + path, e);
            // Non-blocking for the carteira, just return null if photo not found
        } finally {
            closeSession(session);
        }
        return bytes;
    }

    public void closeSession(Session session) {
        if (session != null) {
            try {
                session.logout();
            } catch (Exception e) {
                log.error("Erro ao fechar sessao JCR", e);
            }
        }
    }
}

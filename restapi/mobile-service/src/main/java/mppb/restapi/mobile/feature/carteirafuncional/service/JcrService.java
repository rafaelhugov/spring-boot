package mppb.restapi.cursos.feature.carteirafuncional.service;

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

    @Value("${jcr.url://10.94.0.15:54272/jackrabbit.repository}")
    private String url;

    @Value("${jcr.user:admin}")
    private String user;

    @Value("${jcr.password:admin}")
    private String password;

    protected Session getSession() {
        try {
            Repository repository = new RMIRemoteRepository(url);
            SimpleCredentials creds = new SimpleCredentials(user, password.toCharArray());
            return repository.login(creds, "default");
        } catch (Exception e) {
            log.error("Erro ao conectar no Jackrabbit JCR", e);
            throw new RuntimeException("Erro ao obter sessao JCR", e);
        }
    }

    public byte[] getBytes(String path) {
        Session session = null;
        byte[] bytes = null;

        try {
            session = getSession();
            Node root = session.getRootNode();
            String dir = path.substring(1, path.lastIndexOf("/"));
            String name = path.substring(path.lastIndexOf("/") + 1);
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

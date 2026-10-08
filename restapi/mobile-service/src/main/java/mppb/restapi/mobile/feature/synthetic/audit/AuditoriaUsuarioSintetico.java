package mppb.restapi.mobile.feature.synthetic.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;
import mppb.restapi.mobile.feature.synthetic.model.OrigemConta;
import mppb.restapi.mobile.feature.synthetic.model.PerfilFuncional;

@Component
@ConditionalOnProperty(name = "uat.synthetic-users.enabled", havingValue = "true")
public class AuditoriaUsuarioSintetico {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaUsuarioSintetico.class);

    public void registrar(IdentidadeSintetica identidade, String operacao) {
        if (identidade == null) {
            log.info("usuarioSintetico=true origem=DESCONHECIDA perfil=DESCONHECIDO operacao={}", operacao);
            return;
        }
        registrar(identidade.getOrigem(), identidade.getPerfil(), operacao);
    }

    public void registrar(OrigemConta origem, PerfilFuncional perfil, String operacao) {
        log.info("usuarioSintetico=true origem={} perfil={} operacao={}",
                origem != null ? origem.name() : "N/A",
                perfil != null ? perfil.name() : "N/A",
                operacao != null ? operacao : "N/A");
    }

    public void registrarFalha(String loginTentado, String motivo) {
        log.warn("usuarioSintetico=true loginTentado={} resultado=REJEITADO motivo={}",
                loginTentado != null ? loginTentado : "N/A",
                motivo != null ? motivo : "N/A");
    }

    public void registrarStoreReview(IdentidadeSintetica identidade, String operacao) {
        String origem = "DESCONHECIDA";
        String perfil = "DESCONHECIDO";
        if (identidade != null) {
            origem = switch (identidade.getOrigem()) {
                case REVIEW_GOOGLE -> "GOOGLE";
                case REVIEW_APPLE -> "APPLE";
                default -> identidade.getOrigem().name();
            };
            perfil = identidade.getPerfil() != null ? identidade.getPerfil().name() : "DESCONHECIDO";
        }
        log.info("storeReview=true origem={} perfil={} operacao={}", origem, perfil, operacao);
    }

    public void registrarFalhaStoreReview(String loginTentado, String motivo) {
        log.warn("storeReview=true loginTentado={} resultado=REJEITADO motivo={}",
                loginTentado != null ? loginTentado : "N/A",
                motivo != null ? motivo : "N/A");
    }
}

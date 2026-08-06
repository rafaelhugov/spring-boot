package br.gov.mppb.ncap.feature.formtemplate;

import br.gov.mppb.ncap.feature.formtemplate.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormTemplateDecorationService {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Data
    public static class FormDecoration {
        private Long formId;
        private Map<String, SecaoDecoration> secoes = new HashMap<>();
        private Map<String, PerguntaDecoration> perguntas = new HashMap<>();
    }

    @Data
    public static class SecaoDecoration {
        private Map<String, RoleDecoration> roles = new HashMap<>();
    }

    @Data
    public static class PerguntaDecoration {
        private String campoCustomizado;
        private String preFill;
        private Map<String, RoleDecoration> roles = new HashMap<>();
    }

    @Data
    public static class RoleDecoration {
        private Boolean visible;
        private Boolean editable;
    }

    public void decorar(FormTemplateDto templateDto) {
        if (templateDto == null) {
            return;
        }

        String role = obterRoleUsuarioAtual();
        log.info("Decorando template de formulário ID: {} para a role: {}", templateDto.getId(), role);

        FormDecoration decoration = carregarDecoracoes(templateDto.getId());
        if (decoration == null) {
            // Se não houver arquivo de decoração, assume visível e editável padrão (true) para todas as seções e perguntas
            if (templateDto.getSecoes() != null) {
                templateDto.getSecoes().forEach(s -> {
                    s.setVisible(true);
                    s.setEditable(true);
                    if (s.getBlocos() != null) {
                        s.getBlocos().forEach(b -> {
                            if (b.getPerguntas() != null) {
                                b.getPerguntas().forEach(p -> {
                                    p.setVisible(true);
                                    p.setEditable(true);
                                });
                            }
                        });
                    }
                });
            }
            return;
        }

        if (templateDto.getSecoes() != null) {
            templateDto.getSecoes().forEach(secao -> {
                // Tenta encontrar decoração para a seção
                SecaoDecoration secaoDec = encontrarSecaoDecoration(decoration, secao.getNome());
                if (secaoDec != null && secaoDec.getRoles() != null && secaoDec.getRoles().containsKey(role)) {
                    RoleDecoration roleDec = secaoDec.getRoles().get(role);
                    secao.setVisible(roleDec.getVisible() == null || roleDec.getVisible());
                    secao.setEditable(roleDec.getEditable() == null || roleDec.getEditable());
                } else {
                    secao.setVisible(true);
                    secao.setEditable(true);
                }

                if (secao.getBlocos() != null) {
                    secao.getBlocos().forEach(bloco -> {
                        if (bloco.getPerguntas() != null) {
                            bloco.getPerguntas().forEach(pergunta -> {
                                PerguntaDecoration perguntaDec = encontrarPerguntaDecoration(decoration, pergunta);
                                if (perguntaDec != null) {
                                    pergunta.setCampoCustomizado(perguntaDec.getCampoCustomizado());
                                    pergunta.setPreFillValue(perguntaDec.getPreFill());

                                    if (perguntaDec.getRoles() != null && perguntaDec.getRoles().containsKey(role)) {
                                        RoleDecoration roleDec = perguntaDec.getRoles().get(role);
                                        pergunta.setVisible(roleDec.getVisible() == null || roleDec.getVisible());
                                        pergunta.setEditable(roleDec.getEditable() == null || roleDec.getEditable());
                                    } else {
                                        pergunta.setVisible(true);
                                        pergunta.setEditable(true);
                                    }
                                } else {
                                    pergunta.setVisible(true);
                                    pergunta.setEditable(true);
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private SecaoDecoration encontrarSecaoDecoration(FormDecoration decoration, String nomeSecao) {
        if (nomeSecao == null || decoration.getSecoes() == null) {
            return null;
        }
        for (Map.Entry<String, SecaoDecoration> entry : decoration.getSecoes().entrySet()) {
            if (nomeSecao.equalsIgnoreCase(entry.getKey()) || nomeSecao.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private PerguntaDecoration encontrarPerguntaDecoration(FormDecoration decoration, PerguntaDto pergunta) {
        if (decoration.getPerguntas() == null) {
            return null;
        }
        // Tenta por idcnmp
        if (pergunta.getIdcnmp() != null && decoration.getPerguntas().containsKey(pergunta.getIdcnmp())) {
            return decoration.getPerguntas().get(pergunta.getIdcnmp());
        }
        // Tenta por label
        if (pergunta.getLabel() != null) {
            String label1 = pergunta.getLabel().trim().toLowerCase();
            if (label1.isEmpty()) {
                return null;
            }
            // Primeiro passo: correspondência exata
            for (Map.Entry<String, PerguntaDecoration> entry : decoration.getPerguntas().entrySet()) {
                String label2 = entry.getKey().trim().toLowerCase();
                if (label1.equals(label2)) {
                    return entry.getValue();
                }
            }
            // Segundo passo: correspondência de substring (somente se o label tiver pelo menos 4 caracteres)
            if (label1.length() >= 4) {
                for (Map.Entry<String, PerguntaDecoration> entry : decoration.getPerguntas().entrySet()) {
                    String label2 = entry.getKey().trim().toLowerCase();
                    if (label1.contains(label2) || label2.contains(label1)) {
                        return entry.getValue();
                    }
                }
            }
        }
        return null;
    }

    private FormDecoration carregarDecoracoes(Long templateId) {
        String path = "classpath:templates-cnmp/form_" + templateId + "_decorations.json";
        try {
            Resource resource = resourceLoader.getResource(path);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return objectMapper.readValue(is, FormDecoration.class);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao carregar decorações para o template ID: {} no caminho {}", templateId, path, e);
        }
        return null;
    }

    public String obterRoleUsuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "DELEGACIA";
        }

        try {
            Optional<TokenAuthentication> tokenAuthOpt = SecurityUtils.getAuthenticationToken();
            if (tokenAuthOpt.isPresent()) {
                TokenAuthentication tokenAuth = tokenAuthOpt.get();
                if (tokenAuth.getAcessoData() != null && tokenAuth.getAcessoData().getPerfis() != null) {
                    boolean hasCorregedoria = tokenAuth.getAcessoData().getPerfis().stream()
                            .anyMatch(p -> p.getNome() != null && p.getNome().toUpperCase().contains("CORREGEDORIA"));
                    if (hasCorregedoria) {
                        return "CORREGEDORIA";
                    }

                    boolean hasDelegacia = tokenAuth.getAcessoData().getPerfis().stream()
                            .anyMatch(p -> p.getNome() != null && p.getNome().toUpperCase().contains("DELEGACIA"));
                    if (hasDelegacia) {
                        return "DELEGACIA";
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao ler perfis de TokenAuthentication, usando fallback de authorities", e);
        }

        boolean isCorregedoria = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().toUpperCase().contains("CORREGEDORIA"));
        if (isCorregedoria) {
            return "CORREGEDORIA";
        }

        boolean isDelegacia = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().toUpperCase().contains("DELEGACIA"));
        if (isDelegacia) {
            return "DELEGACIA";
        }

        return "MEMBRO";
    }
}

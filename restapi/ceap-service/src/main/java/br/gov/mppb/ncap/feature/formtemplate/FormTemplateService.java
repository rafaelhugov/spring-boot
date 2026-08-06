package br.gov.mppb.ncap.feature.formtemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormTemplateService {

    private final FormTemplateRepository repository;
    private final FormTemplateSincronizacaoService syncService;

    @Transactional(readOnly = true)
    public List<FormTemplate> findAllActive() {
        log.info("Buscando todos os templates de formulários ativos.");
        return repository.findAll().stream()
                .filter(FormTemplate::getAtivo)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<FormTemplate> findById(Long id) {
        log.info("Buscando template de formulário ID: {}", id);
        Optional<FormTemplate> templateOpt = repository.findById(id);
        if (templateOpt.isPresent()) {
            // Forçar carregamento das coleções Lazy dentro da transação
            FormTemplate template = templateOpt.get();
            template.getSecoes().forEach(secao -> {
                secao.getBlocos().forEach(bloco -> {
                    bloco.getPerguntas().forEach(pergunta -> {
                        pergunta.getOpcoes().size();
                    });
                });
            });
        }
        return templateOpt;
    }

    public void sincronizar() {
        log.info("Disparando sincronização manual de templates de formulários.");
        syncService.sincronizarTemplates();
    }
}

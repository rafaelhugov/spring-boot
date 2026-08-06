package br.gov.mppb.ncap.feature.formtemplate;

import br.gov.mppb.ncap.feature.entidade.integration.CnmpClient;
import br.gov.mppb.ncap.feature.entidade.integration.CnmpClient.CNMPDetalheFormularioResponse;
import br.gov.mppb.ncap.feature.entidade.integration.CnmpClient.CNMPCampoResponse;
import br.gov.mppb.ncap.feature.entidade.integration.CnmpClient.CNMPSecaoResponse;
import br.gov.mppb.ncap.feature.entidade.integration.CnmpClient.CNMPOpcaoRespostaResponse;
import br.gov.mppb.ncap.feature.entidade.integration.CnmpClient.CNMPDependenciaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormTemplateSincronizacaoService {

    private final CnmpClient cnmpClient;
    private final FormTemplateRepository formTemplateRepository;

    @Value("${api.cnmp.forms:1108,1125}")
    private List<Long> formIds;

    @Transactional
    public void sincronizarTemplates() {
        log.info("Iniciando sincronização de templates de formulários do CNMP...");

        if (formIds == null || formIds.isEmpty()) {
            log.warn("Nenhum ID de formulário configurado para sincronização.");
            return;
        }

        for (Long formId : formIds) {
            try {
                sincronizarTemplate(formId);
            } catch (Exception e) {
                log.error("Erro ao sincronizar template de formulário ID: {}", formId, e);
            }
        }
    }

    private void sincronizarTemplate(Long formId) {
        Optional<FormTemplate> existenteOpt = formTemplateRepository.findById(formId);
        if (existenteOpt.isPresent()) {
            log.info("Template de formulário ID: {} já existe localmente. Pulando criação.", formId);
            return;
        }

        log.info("Carregando detalhes do formulário ID: {} a partir do classpath local...", formId);
        String resourcePath = "templates-cnmp/form_" + formId + ".json";

        CNMPDetalheFormularioResponse dto;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                log.warn("Arquivo de template não encontrado no classpath: {}", resourcePath);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            dto = mapper.readValue(inputStream, CNMPDetalheFormularioResponse.class);
        } catch (Exception e) {
            log.error("Falha ao ler ou converter o arquivo JSON local do template {}", formId, e);
            return;
        }

        if (dto == null) {
            log.warn("Não foi possível obter os detalhes do formulário ID: {}", formId);
            return;
        }

        String tipoEntidade = "DESCONHECIDO";
        if (dto.getTiposEntidadeAceitos() != null && !dto.getTiposEntidadeAceitos().isEmpty()) {
            tipoEntidade = dto.getTiposEntidadeAceitos().get(0).getDescricao();
        }

        FormTemplate template = new FormTemplate();
        template.setId(dto.getId());
        template.setResolucao(dto.getAmbiente());
        template.setTipoentidade(tipoEntidade);
        template.setAtivo(true);

        Map<Long, Pergunta> perguntasPorIdCnmp = new HashMap<>();

        if (dto.getSecoes() != null) {
            for (CNMPSecaoResponse secaoDto : dto.getSecoes()) {
                Secao secao = new Secao();
                secao.setTemplate(template);
                secao.setNome(secaoDto.getNome());
                secao.setOrdem(secaoDto.getIndice() != null ? secaoDto.getIndice() : 1);

                // Criar Bloco padrão para conter as perguntas
                Bloco blocoUnico = new Bloco();
                blocoUnico.setSecao(secao);
                blocoUnico.setNome("Geral");
                blocoUnico.setOrdem(1);

                if (secaoDto.getCampos() != null) {
                    // Primeiro Passo: Criar perguntas e mapeá-las em memória
                    for (CNMPCampoResponse campoDto : secaoDto.getCampos()) {
                        Pergunta pergunta = new Pergunta();
                        pergunta.setBloco(blocoUnico);
                        pergunta.setIdcnmp(campoDto.getCodigo());
                        pergunta.setLabel(campoDto.getLabel() != null ? campoDto.getLabel() : campoDto.getDescricao());
                        pergunta.setCampotipo(campoDto.getTipoCampo() != null ? campoDto.getTipoCampo().getTipo() : "TEXTO");
                        pergunta.setObrigatorio(campoDto.getObrigatorio() != null ? campoDto.getObrigatorio() : false);
                        pergunta.setExportarcnmp(true);
                        pergunta.setOrdem(campoDto.getIndice() != null ? campoDto.getIndice() : 1);
                        pergunta.setTabulacao(campoDto.getTabulacao() != null ? campoDto.getTabulacao() : 0);

                        // Mapear opções de resposta
                        if (campoDto.getRespostas() != null) {
                            for (CNMPOpcaoRespostaResponse opcaoDto : campoDto.getRespostas()) {
                                OpcaoResposta opcao = new OpcaoResposta();
                                opcao.setPergunta(pergunta);
                                opcao.setDescricao(opcaoDto.getDescricao());
                                opcao.setValor(opcaoDto.getValor());
                                pergunta.getOpcoes().add(opcao);
                            }
                        }

                        perguntasPorIdCnmp.put(campoDto.getId(), pergunta);
                        blocoUnico.getPerguntas().add(pergunta);
                    }

                    // Segundo Passo: Mapear dependências de perguntas
                    for (CNMPCampoResponse campoDto : secaoDto.getCampos()) {
                        if (campoDto.getDependencias() != null && !campoDto.getDependencias().isEmpty()) {
                            CNMPDependenciaResponse depDto = campoDto.getDependencias().get(0);
                            Pergunta child = perguntasPorIdCnmp.get(campoDto.getId());
                            Pergunta parent = perguntasPorIdCnmp.get(depDto.getIdCampo());
                            if (child != null && parent != null) {
                                child.setDependenciapai(parent);
                                child.setDependenciavalor(depDto.getValorResposta());
                            }
                        }
                    }
                }

                secao.getBlocos().add(blocoUnico);
                template.getSecoes().add(secao);
            }
        }

        formTemplateRepository.save(template);
        log.info("Template de formulário ID: {} ('{}') sincronizado com sucesso.", formId, dto.getNome());
    }
}

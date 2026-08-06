package br.gov.mppb.ncap.feature.entidade.integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CnmpClient {

    @Value("${api.cnmp.base-url:}")
    private String baseUrl;

    @Value("${api.cnmp.username:}")
    private String username;

    @Value("${api.cnmp.password:}")
    private String password;

    @Value("${api.cnmp.forms:1108,1125}")
    private List<Long> formIds;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<CnmpEntidadeDto> obterUnidadesOficiais() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            log.warn("A URL base da API do CNMP não está configurada. Retornando unidades mockadas para desenvolvimento.");
            return List.of(
                CnmpEntidadeDto.builder().id(1L).nomecnmp("1ª Delegacia Distrital - João Pessoa").tipoentidade("POLICIA_CIVIL").ativa(true).municipio("João Pessoa").build(),
                CnmpEntidadeDto.builder().id(2L).nomecnmp("2ª Delegacia Distrital - Campina Grande").tipoentidade("POLICIA_CIVIL").ativa(true).municipio("Campina Grande").build()
            );
        }

        List<CnmpEntidadeDto> resultado = new ArrayList<>();
        if (formIds == null || formIds.isEmpty()) {
            log.warn("Nenhum formulário configurado para busca no CNMP.");
            return resultado;
        }

        for (Long formId : formIds) {
            log.info("Buscando unidades do CNMP para o formulário ID: {}", formId);
            List<CnmpEntidadeDto> unidadesDoForm = obterUnidadesDoFormulario(formId);
            resultado.addAll(unidadesDoForm);
        }

        return resultado;
    }

    private List<CnmpEntidadeDto> obterUnidadesDoFormulario(Long formId) {
        String url = baseUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "entidades?formulario=" + formId;

        HttpHeaders headers = new HttpHeaders();
        if (username != null && !username.trim().isEmpty()) {
            headers.setBasicAuth(username, password);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<CNMPEntidadeResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<CNMPEntidadeResponse>>() {}
            );
            List<CNMPEntidadeResponse> body = response.getBody();
            if (body == null) {
                return List.of();
            }

            return body.stream().map(e -> CnmpEntidadeDto.builder()
                    .id(e.getId())
                    .nomecnmp(e.getNome())
                    .tipoentidade(e.getTipoEntidade() != null ? e.getTipoEntidade().getDescricao() : "DESCONHECIDO")
                    .municipio(e.getMunicipio())
                    .ativa(e.getAtiva() != null ? e.getAtiva() : true)
                    .build()
            ).collect(Collectors.toList());
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.error("Erro de autenticação (401) ao acessar a API do CNMP para o formulário {}", formId, e);
            throw new RuntimeException("Falha na autenticação com a API do CNMP. Verifique as credenciais configuradas.", e);
        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden e) {
            log.error("Acesso proibido (403) ao acessar a API do CNMP para o formulário {}", formId, e);
            throw new RuntimeException("Acesso negado à API do CNMP. Verifique as permissões das credenciais.", e);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Erro HTTP {} retornado pela API do CNMP para o formulário {}", e.getStatusCode(), formId, e);
            throw new RuntimeException("Erro retornado pela API do CNMP: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Falha de conexão ao acessar a API do CNMP para o formulário {}", formId, e);
            throw new RuntimeException("Não foi possível conectar à API do CNMP (Timeout/Rede).", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao obter unidades do CNMP para o formulário {}", formId, e);
            throw new RuntimeException("Erro inesperado na integração com o CNMP: " + e.getMessage(), e);
        }
    }

    @Deprecated
    public CNMPDetalheFormularioResponse obterDetalheFormulario(Long formId) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("A URL base da API do CNMP não está configurada.");
        }

        String url = baseUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "formularios/" + formId;

        HttpHeaders headers = new HttpHeaders();
        if (username != null && !username.trim().isEmpty()) {
            headers.setBasicAuth(username, password);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CNMPDetalheFormularioResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    CNMPDetalheFormularioResponse.class
            );
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.error("Erro de autenticação (401) ao obter detalhes do formulário {} do CNMP", formId, e);
            throw new RuntimeException("Falha na autenticação com a API do CNMP. Verifique as credenciais configuradas.", e);
        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden e) {
            log.error("Acesso proibido (403) ao obter detalhes do formulário {} do CNMP", formId, e);
            throw new RuntimeException("Acesso negado à API do CNMP. Verifique as permissões das credenciais.", e);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Erro HTTP {} retornado pela API do CNMP ao obter detalhes do formulário {}", e.getStatusCode(), formId, e);
            throw new RuntimeException("Erro retornado pela API do CNMP: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Falha de conexão ao obter detalhes do formulário {} do CNMP", formId, e);
            throw new RuntimeException("Não foi possível conectar à API do CNMP (Timeout/Rede).", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao obter detalhes do formulário {} do CNMP", formId, e);
            throw new RuntimeException("Erro inesperado na integração com o CNMP: " + e.getMessage(), e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CNMPEntidadeResponse {
        private long id;
        private String nome;
        private Boolean ativa;
        private String municipio;
        private CNMPEntidadeTipo tipoEntidade;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CNMPEntidadeTipo {
        private long id;
        private String descricao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CNMPDetalheFormularioResponse {
        private Long id;
        private String nome;
        private String ambiente;
        private Integer versao;
        private String periodicidade;
        private Integer anoInicio;
        private Integer periodoInicio;
        private List<CNMPEntidadeTipo> tiposEntidadeAceitos;
        private List<CNMPSecaoResponse> secoes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CNMPSecaoResponse {
        private Long id;
        private Integer indice;
        private String nome;
        private List<CNMPCampoResponse> campos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CNMPCampoResponse {
        private Long id;
        private String codigo;
        private String descricao;
        private String label;
        private Integer indice;
        private Integer tabulacao;
        private Boolean obrigatorio;
        private CNMPTipoCampoResponse tipoCampo;
        private List<CNMPDependenciaResponse> dependencias;
        private List<CNMPOpcaoRespostaResponse> respostas;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CNMPTipoCampoResponse {
        private String tipo;
        private String descricao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CNMPDependenciaResponse {
        private Long idCampo;
        private String valorResposta;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CNMPOpcaoRespostaResponse {
        private String valor;
        private String descricao;
    }
}

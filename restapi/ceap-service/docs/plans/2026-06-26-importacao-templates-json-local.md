# Importação de Templates via JSON Local - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Refatorar a sincronização de templates do NCAP para carregar os formulários a partir de arquivos JSON locais no classpath (eliminando dependência HTTP em runtime da API do CNMP) e garantir o correto mapeamento de tipos de entidade e perguntas.

**Architecture:** Modificar `FormTemplateSincronizacaoService` para ler os esquemas dos formulários de `/templates-cnmp/form_{id}.json` utilizando `ObjectMapper` do Jackson, mapeando o grafo completo para as tabelas locais do NCAP e tornando os testes unitários independentes de rede.

**Tech Stack:** Java 17, Spring Boot 3.x, Jackson (ObjectMapper), JPA/Hibernate.

---

### Task 1: Preparar os Recursos de Template JSON
Criar os arquivos de template locais no classpath para o ambiente de testes e de execução.

**Files:**
- Create: `backend/src/test/resources/templates-cnmp/form_1108.json`
- Create: `backend/src/main/resources/templates-cnmp/form_1567.json`
- Create: `backend/src/main/resources/templates-cnmp/form_1566.json`
- Create: `backend/src/main/resources/templates-cnmp/form_1565.json`
- Create: `backend/src/main/resources/templates-cnmp/form_1562.json`

**Step 1: Criar o arquivo JSON de teste `form_1108.json`**
Criar o arquivo `backend/src/test/resources/templates-cnmp/form_1108.json` representando a estrutura esperada pelo teste de sincronização:

```json
{
  "id": 1108,
  "nome": "Formulário Visita Técnica à Delegacia Estadual",
  "ambiente": "Resolução 279",
  "versao": 1,
  "periodicidade": "Semestral",
  "anoInicio": 2017,
  "periodoInicio": 1,
  "tiposEntidadeAceitos": [
    {
      "id": 250,
      "descricao": "POLICIA_CIVIL"
    }
  ],
  "secoes": [
    {
      "id": 1,
      "indice": 1,
      "nome": "Informações Gerais",
      "campos": [
        {
          "id": 101,
          "codigo": "TXT_OBSERVACOES_FINAIS_REPRESENTANTE_MINISTERIO_PUBLICO",
          "descricao": "Observações Finais",
          "label": "Observações Finais",
          "indice": 1,
          "tabulacao": 0,
          "obrigatorio": true,
          "tipoCampo": {
            "tipo": "TEXTO",
            "descricao": "Campo texto"
          }
        },
        {
          "id": 102,
          "codigo": "PERG_EFETIVO_TOTAL",
          "descricao": "Efetivo Total",
          "label": "Efetivo Total",
          "indice": 2,
          "tabulacao": 0,
          "obrigatorio": false,
          "tipoCampo": {
            "tipo": "INTEIRO",
            "descricao": "Inteiro"
          }
        },
        {
          "id": 103,
          "codigo": "PERG_POSSUI_VIATURA",
          "descricao": "Possui Viatura?",
          "label": "Possui Viatura?",
          "indice": 3,
          "tabulacao": 0,
          "obrigatorio": false,
          "tipoCampo": {
            "tipo": "RADIO",
            "descricao": "Seleção Única"
          },
          "respostas": [
            {
              "valor": "SIM",
              "descricao": "Sim"
            },
            {
              "valor": "NAO",
              "descricao": "Não"
            }
          ]
        }
      ]
    }
  ]
}
```

*Nota: Para fins de desenvolvimento inicial, os arquivos reais `form_1567.json`, `form_1566.json`, `form_1565.json` e `form_1562.json` serão inicializados com estruturas válidas ou cópias do mock da delegacia estadual para evitar arquivos nulos.*

**Step 2: Commitar**
```bash
git add backend/src/test/resources/templates-cnmp/form_1108.json
git commit -m "test: add mock template 1108 for unit test verification"
```

---

### Task 2: Modificar o Serviço de Sincronização
Alterar o `FormTemplateSincronizacaoService` para carregar a estrutura do arquivo JSON local ao invés de usar o HTTP client.

**Files:**
- Modify: `backend/src/main/java/br/gov/mppb/ncap/feature/formtemplate/FormTemplateSincronizacaoService.java`

**Step 1: Escrever código do carregador**
Alterar a lógica interna do método `sincronizarTemplate` para ler do classpath:

```java
// Substituir a chamada do cnmpClient por leitura local
private void sincronizarTemplate(Long formId) {
    Optional<FormTemplate> existenteOpt = formTemplateRepository.findById(formId);
    if (existenteOpt.isPresent()) {
        log.info("Template de formulário ID: {} já existe localmente. Pulando criação.", formId);
        return;
    }

    log.info("Carregando template de formulário ID: {} a partir do classpath local...", formId);
    String resourcePath = "templates-cnmp/form_" + formId + ".json";
    
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
        if (inputStream == null) {
            log.warn("Arquivo de template não encontrado no classpath: {}", resourcePath);
            return;
        }

        var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        CNMPDetalheFormularioResponse dto = objectMapper.readValue(inputStream, CNMPDetalheFormularioResponse.class);
        
        // Mapear DTO para entidade e salvar no banco (o resto da lógica existente de mapeamento de seções/perguntas se mantém idêntica)
        // ...
    } catch (Exception e) {
        log.error("Falha ao ler ou converter o arquivo JSON local do template {}", formId, e);
    }
}
```

**Step 2: Executar testes locais para verificar se passa**
Executar os testes e validar se `FormTemplateSincronizacaoServiceTest` executa perfeitamente sem bater na API externa.
Run: `mvn test -Dtest=FormTemplateSincronizacaoServiceTest`
Expected: PASS

**Step 3: Commitar**
```bash
git add backend/src/main/java/br/gov/mppb/ncap/feature/formtemplate/FormTemplateSincronizacaoService.java
git commit -m "feat: refactor template sync service to load from local classpath JSONs"
```

---

### Task 3: Desacoplar o Método Depreciado do CnmpClient
Marcar o método `obterDetalheFormulario` do `CnmpClient` como `@Deprecated` para indicar que a carga de detalhes agora ocorre via JSON local, evitando requisições HTTP redundantes para obter esquemas estáticos em produção.

**Files:**
- Modify: `backend/src/main/java/br/gov/mppb/ncap/feature/unidade/integration/CnmpClient.java`

**Step 1: Aplicar anotação de depreciação**
Adicionar a anotação `@Deprecated` no método `obterDetalheFormulario(Long formId)`.

**Step 2: Executar build e testes completos do projeto**
Run: `mvn clean test`
Expected: PASS

**Step 3: Commitar**
```bash
git add backend/src/main/java/br/gov/mppb/ncap/feature/unidade/integration/CnmpClient.java
git commit -m "refactor: deprecate obtaining template details from remote API"
```

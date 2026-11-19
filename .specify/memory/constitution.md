<!--
Relatório de impacto da atualização
Versão: modelo inicial sem versão → 1.0.0.
Princípios: substituição dos campos do modelo pelos nove princípios aprovados.
Seções adicionadas: organização das especificações e do Spec Kit; fluxo de trabalho
 e critérios de entrega; governança específica do repositório.
Removidos: instruções de exemplo e campos não preenchidos do modelo inicial.
Pendência operacional: validar a personalização do Spec Kit para specs por subprojeto.
Nenhum script oficial, habilidade ou modelo foi alterado nesta atualização.
Relatório temporário de revisão: remover antes de versionar a constituição.
-->

# Constituição Global do Repositório Spring Boot/Java/Maven

## Princípios fundamentais

### I. Constituição única e isolamento entre subprojetos

Todos os subprojetos backend deste repositório devem seguir esta constituição global, mantida em `.specify/memory/constitution.md`, na raiz `spring-boot/`.

A instalação do Spec Kit deve permanecer centralizada nessa raiz. Não devem ser criadas constituições ou instalações independentes nos subprojetos.

Cada subprojeto em `restapi/` deve preservar suas responsabilidades, regras de negócio, configurações e contratos. Toda alteração deve identificar o subprojeto afetado e respeitar o escopo autorizado.

O compartilhamento de código por módulos como `restapi-common` deve atender a necessidades efetivamente comuns. Regras exclusivas de um serviço devem permanecer nesse serviço. Alterações compartilhadas exigem análise dos consumidores afetados.

Orientações locais podem complementar esta constituição, sem contrariar seus princípios.

### II. Arquitetura organizada por funcionalidade

A organização de referência é a existente em:

```text
restapi/cursos-service/src/main/java/mppb/restapi/cursos/feature/curso/
```

Novas funcionalidades devem ser organizadas sob o pacote-base do respectivo subprojeto:

```text
src/main/java/<pacote-base>/feature/<funcionalidade>/
├── controller/
├── converter/
├── dto/
├── entity/
├── enums/
├── job/
├── repository/
├── service/
└── specification/
```

Os pacotes devem ser criados conforme a necessidade da funcionalidade. Não é obrigatório criar diretórios vazios ou classes artificiais para preencher a estrutura.

| Pacote | Responsabilidade |
|---|---|
| `controller` | Receber requisições, validar entradas e produzir respostas HTTP, delegando regras de negócio. |
| `converter` | Converter representações, especialmente entre entidades e DTOs, sem executar regras de negócio ou persistência. |
| `dto` | Representar os contratos de entrada e saída e suas validações estruturais. |
| `entity` | Representar entidades e seus mapeamentos de persistência. |
| `enums` | Representar conjuntos fechados de valores do domínio. |
| `job` | Coordenar execuções agendadas, delegando regras de negócio aos serviços. |
| `repository` | Encapsular consultas e operações de persistência. |
| `service` | Implementar casos de uso, regras de negócio e limites transacionais. |
| `specification` | Compor critérios de consulta, como Spring Data JPA Specifications. |

O pacote Java `specification` não se confunde com a pasta documental `specs/` do Spec Kit.

Pacotes adicionais, como os destinados a validação, integração ou configuração, devem ter responsabilidade explícita e seguir as convenções do subprojeto.

Dependências devem ser explícitas, preferencialmente por injeção via construtor. Controllers e tarefas agendadas não devem concentrar regras de negócio nem acessar diretamente a persistência para executar casos de uso.

A adoção desse padrão não autoriza reorganização em massa do código existente. Adequações devem ocorrer incrementalmente, dentro de escopo aprovado.

### III. Maven e gestão de dependências

O repositório utiliza um `pom.xml` compartilhado em `restapi/` e um `pom.xml` por subprojeto.

Configurações comuns, versões e gerenciamento de dependências devem utilizar o POM compartilhado quando aplicáveis. Cada subprojeto deve declarar as dependências necessárias ao seu funcionamento.

A configuração efetiva de herança e agregação deve ser verificada antes de alterações ou comandos. A existência de um diretório em `restapi/` não significa que ele esteja registrado como módulo no POM agregador.

É obrigatório:

- Evitar versões divergentes de dependências sem justificativa técnica.
- Não adicionar bibliotecas quando os recursos existentes atendem ao requisito.
- Avaliar compatibilidade e impacto antes de atualizar Java, Spring Boot, bibliotecas ou plugins.
- Considerar os consumidores ao alterar o POM compartilhado ou módulos comuns.
- Executar verificações Maven no escopo adequado, incluindo dependências necessárias.

Atualizações gerais de versões e alterações na composição dos módulos exigem escopo explícito.

### IV. Segurança e proteção de dados

A segurança deve ser garantida no backend, independentemente das validações dos consumidores.

Autenticação e autorização devem utilizar os mecanismos de Spring Security adotados pelo subprojeto. Recursos protegidos devem negar acesso quando não houver autorização válida.

As verificações devem considerar a operação e o recurso solicitado, incluindo propriedade, vínculo, perfil e sigilo quando aplicáveis. Conhecer o identificador de um recurso não concede autorização para acessá-lo.

Entradas devem ser validadas quanto a formato, tamanho, limites e regras de negócio. Consultas devem utilizar mecanismos seguros de parametrização.

Credenciais, tokens e segredos não devem ser incluídos em URLs, código versionado, exemplos documentais ou registros de execução. Sua obtenção deve ocorrer por configuração externa apropriada.

Erros enviados aos consumidores não devem expor detalhes internos ou dados sensíveis. Recursos de simulação de autenticação devem ficar restritos aos ambientes autorizados de desenvolvimento e teste.

Recebimento e disponibilização de arquivos devem considerar tipos permitidos, limites, nomes seguros e autorização sobre o recurso associado.

Mudanças de segurança devem verificar cenários de acesso permitido e negado. Não se deve desativar uma proteção apenas para contornar falhas de implementação ou testes.

### V. Clean Code e simplicidade

O código deve apresentar intenção clara, responsabilidades coesas e nomenclatura consistente com o domínio e o subprojeto.

Métodos e classes devem ser mantidos compreensíveis. Duplicações relevantes devem ser reduzidas sem introduzir abstrações prematuras.

Configurações de ambiente e valores operacionais não devem ser espalhados pelo código. Comentários devem explicar decisões e restrições que não estejam evidentes na implementação.

Exceções devem ser tratadas com contexto suficiente para diagnóstico. É proibido ocultar falhas retornando sucesso ou resultados vazios que modifiquem silenciosamente o significado de uma operação.

Refatorações devem estar relacionadas ao objetivo aprovado. Reformatações extensas, renomeações em massa e alterações arquiteturais independentes da tarefa exigem escopo próprio.

### VI. Contratos de API e integrações externas

APIs devem possuir contratos claros de entrada, saída, erros, autenticação e autorização.

Endpoints devem respeitar a semântica HTTP e as convenções do subprojeto. DTOs devem controlar os dados expostos, evitando o uso direto de entidades de persistência como contratos públicos.

Mudanças incompatíveis exigem identificação dos consumidores, estratégia de transição e aprovação humana.

Integrações externas devem ser encapsuladas em clientes ou adaptadores e devem:

- Obter endereços e credenciais por configuração externa.
- Definir limites de tempo de conexão e resposta.
- Tratar indisponibilidade, respostas inválidas e erros de autenticação.
- Utilizar novas tentativas somente quando os efeitos colaterais e a idempotência estiverem compreendidos.
- Evitar manter transações de banco abertas durante chamadas externas demoradas.
- Produzir registros úteis para diagnóstico, sem exposição de segredos.

Contratos e documentação OpenAPI, quando adotados, devem acompanhar as mudanças. Integrações com consumidores Vue.js e JavaScript devem preservar compatibilidade ou documentar a transição aprovada.

### VII. Português brasileiro e nomes técnicos

Especificações, planos, tarefas, documentação produzida, justificativas e comunicações de trabalho devem utilizar português brasileiro.

Nomes técnicos padronizados, como Spring Boot, Java, Maven, Vue.js, JavaScript, Spec Kit, Spring Security e OpenAPI, devem ser preservados.

Identificadores de código devem seguir as convenções do subprojeto. Nomes de pacotes, bibliotecas, comandos, propriedades e campos de contratos não devem ser traduzidos de maneira que altere seu funcionamento ou compatibilidade.

Mensagens destinadas às pessoas usuárias devem utilizar português brasileiro, respeitando os contratos existentes.

### VIII. Desenvolvimento incremental e aprovação humana

O desenvolvimento deve avançar em incrementos pequenos, verificáveis e relacionados ao objetivo solicitado.

Antes da implementação, devem estar claros o subprojeto, o problema, o escopo, os critérios de aceitação e os impactos relevantes.

Pedidos de análise ou proposta não autorizam modificações. Quando o usuário exigir aprovação prévia, a proposta deve ser apresentada integralmente e a execução deve aguardar aprovação explícita.

A aprovação de uma proposta autoriza as edições e verificações necessárias ao escopo aprovado, sem exigir confirmação repetida para cada etapa. Ampliações materiais de escopo exigem nova aprovação.

Operações destrutivas, alterações incompatíveis em contratos, intervenções em dados de produção e publicação em ambientes compartilhados exigem autorização explícita que cubra essas ações.

Alterações preexistentes devem ser preservadas. A entrega deve distinguir trabalho concluído, verificações realizadas e pendências.

### IX. Testes, qualidade e observabilidade

As verificações devem ser proporcionais ao comportamento alterado e aos riscos envolvidos.

Regras de negócio devem receber testes adequados. Alterações de persistência, contratos e integrações devem receber verificações capazes de demonstrar seu funcionamento real.

Mudanças de segurança devem incluir cenários negativos. Correções de defeitos devem incluir testes de regressão quando tecnicamente viável.

Testes devem verificar comportamentos observáveis. Não devem existir apenas para reproduzir a implementação ou aumentar artificialmente métricas de cobertura.

Alterações documentais e ajustes sem efeito funcional podem ser validados por revisão apropriada, sem criação obrigatória de testes automatizados.

Registros de execução devem permitir diagnóstico e correlação de operações relevantes, respeitando a proteção de dados.

Resultados devem ser informados com precisão. Verificações não executadas ou impedidas pelo ambiente não podem ser apresentadas como aprovadas.

### X. Controle de versão, branch dedicada por tarefa e proibição de trabalho ou merge direto na `master`/`main`

Toda tarefa (nova funcionalidade, correção de bug, ajuste visual, script SQL, configuração ou documentação Spec Kit) deve obrigatoriamente iniciar com a criação de uma branch dedicada (`git checkout -b <tipo>/<slug>`) a partir da `master`/`main` antes de qualquer alteração em arquivos.

É estritamente proibido trabalhar diretamente na branch `master` (ou `main`), realizar commit direto na `master`/`main`, executar `git merge` direto na `master`/`main` local ou publicar (`git push`) merges locais diretamente em `origin/master` ou `github/master`, em qualquer projeto ou subprojeto.

Todo trabalho deve ser realizado, comitado e publicado exclusivamente em sua branch dedicada (`feat/...`, `fix/...`, `chore/...`).

A integração na `master`/`main` ocorre exclusivamente por meio de **Merge Request (GitLab)** e **Pull Request (GitHub)** revisados e aprovados pelo usuário. A branch `master`/`main` local somente pode ser atualizada por `git pull`/`git fetch` para refletir merges já consolidados remotamente pelo usuário.

### XI. Uso obrigatório do Spec Kit para novos ajustes e evoluções

Sempre que o usuário solicitar novos ajustes, correções de comportamento ou evoluções em qualquer funcionalidade, é obrigatório utilizar o **Spec Kit** (criando ou atualizando `specs/<NNN>-<funcionalidade>/spec.md`, `plan.md` e `tasks.md`) antes de iniciar a implementação.

## Organização das especificações e do Spec Kit

A organização desejada é:

```text
spring-boot/
├── .specify/
│   └── memory/
│       └── constitution.md
├── .agents/
│   └── skills/
└── restapi/
    ├── pom.xml
    └── <subprojeto>/
        ├── pom.xml
        ├── src/
        └── specs/
            └── <numero>-<funcionalidade>/
```

A numeração deve ser calculada no âmbito do subprojeto, conforme a convenção validada da personalização do Spec Kit.

A adoção operacional dessa organização depende de validar:

1. A criação da especificação no subprojeto selecionado.
2. A localização da mesma feature nas etapas posteriores do Spec Kit.
3. A utilização da constituição central.
4. A alternância entre features de subprojetos distintos.
5. A preservação dos scripts oficiais.

A alteração das instruções de uma habilidade, isoladamente, não comprova essa validação.

Quando utilizado, `SPECIFY_INIT_DIR` deve apontar para a raiz que contém `.specify/`. `SPECIFY_FEATURE_DIRECTORY` deve identificar a feature pretendida.

O arquivo central `.specify/feature.json` representa uma seleção ativa compartilhada. Trabalhos simultâneos devem utilizar contexto explícito e evitar sobrescrever a seleção de outras tarefas.

A aprovação desta constituição não autoriza migração automática de especificações existentes nem alterações nos scripts oficiais do Spec Kit.

## Fluxo de trabalho e critérios de entrega

Cada tarefa deve seguir as etapas aplicáveis:

1. **Analisar:** identificar o subprojeto, ler as instruções pertinentes e verificar código, dependências e alterações preexistentes.
2. **Especificar:** registrar o comportamento esperado, limites e critérios de aceitação.
3. **Planejar:** definir a abordagem, os arquivos afetados, os impactos compartilhados e as verificações.
4. **Confirmar o escopo autorizado:** verificar se a execução está coberta pelo pedido ou pela aprovação recebida.
5. **Implementar:** realizar o menor incremento suficiente, seguindo a organização por funcionalidade.
6. **Verificar:** executar os testes e demais verificações pertinentes.
7. **Apresentar:** informar o resultado, as evidências, as limitações e as pendências.

Mudanças de banco de dados devem seguir o mecanismo de migração adotado pelo subprojeto e considerar integridade, compatibilidade e recuperação.

Mudanças em `restapi/pom.xml` ou módulos compartilhados devem incluir análise dos subprojetos afetados e validação compatível com esse alcance.

Uma entrega somente pode ser declarada concluída quando os critérios de aceitação forem atendidos e as verificações necessárias tiverem resultado conhecido. Impedimentos devem permanecer explícitos.

## Governança

Esta constituição é a referência global de governança técnica dos subprojetos backend.

Sua aprovação não declara conformidade automática de todo o código existente. Divergências legadas devem ser registradas e tratadas incrementalmente, sem ampliar o escopo de tarefas sem autorização.

Orientações específicas podem detalhar regras locais. Conflitos com esta constituição devem ser apresentados para decisão humana, sem exceções silenciosas.

Exceções devem registrar motivo, alcance, riscos, medidas compensatórias e aprovação. Quando temporárias, devem possuir prazo ou condição de revisão.

Qualquer alteração desta constituição exige apresentação prévia da proposta e aprovação explícita antes da substituição do conteúdo vigente.

O versionamento deve seguir:

- **Versão principal:** remoção ou redefinição incompatível de princípios.
- **Versão secundária:** inclusão de princípios ou ampliação material das exigências.
- **Correção:** ajustes editoriais e esclarecimentos sem mudança de obrigação.

A primeira versão será ratificada na data da aprovação explícita. Alterações posteriores devem preservar a data de ratificação e atualizar a data da última alteração.

**Versão:** 1.2.0
**Ratificação:** 2026-11-10
**Última alteração:** 2026-11-19

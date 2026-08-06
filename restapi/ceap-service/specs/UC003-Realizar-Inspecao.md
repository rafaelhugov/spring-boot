# UC003 - Realizar Inspeção e Retificação

Este caso de uso descreve o processo de preenchimento do formulário físico da inspeção pelo Membro (Promotor de Justiça), a auditoria/parecer técnico da Corregedoria, e o fluxo assistido de retificação em caso de devolução de campos inconsistentes.

---

## 📋 Informações Gerais

* **ID/Código:** UC003
* **Nome:** Realizar Inspeção e Retificação
* **Versão:** 1.2
* **Status:** Implementado
* **Atores:** Promotor de Justiça (Membro) / Corregedor (Corregedoria)

---

## 📌 Pré-condições & Pós-condições

### Pré-condições
1. O ator deve estar devidamente autenticado e ter a role corporativa correspondente (`ROLE_PROMOTOR` ou `ROLE_CORREGEDORIA`).
2. A inspeção deve estar previamente agendada e com status ativo (`AGENDADA` ou `EM_PREENCHIMENTO`).
3. O template oficial do CNMP (ex: ID `1567`) correspondente ao tipo da unidade policial visitada deve estar carregado na base de dados.

### Pós-condições
1. Os dados inseridos no questionário são salvos na tabela `ncap.respostasinspecao`.
2. Se submetido pelo Membro, o status transita para `VALIDACAO_CORREGEDORIA` e o formulário fica bloqueado para edições do Membro.
3. Se devolvido pela Corregedoria com pareceres/glosas, o status transita para `CORRIGIDO_DELEGACIA` e o formulário é reaberto para edição controlada do Membro.
4. Se homologado pela Corregedoria, o status transita para `CONCLUIDA` e o formulário é trancado em definitivo.

---

## 🔄 Fluxo de Eventos

### Fluxo Principal (Preenchimento e Envio pelo Membro)
1. O Membro acessa a inspeção agendada na listagem de vistorias.
2. O sistema carrega a tela do formulário dinâmico (`InspecaoPreenchimentoView.vue`) organizando as perguntas em seções conforme o template.
3. O Membro realiza o preenchimento das respostas e, opcionalmente, adiciona **Anotações Pré-Visita** nos campos.
4. O sistema exibe o progresso de preenchimento no menu de seções lateral e na barra de progresso central.
5. O Membro clica em **Submeter à Corregedoria**.
6. O sistema executa as validações, altera o status para `VALIDACAO_CORREGEDORIA` e bloqueia os campos de resposta.

### Fluxo Alternativo A (Análise e Devolução pela Corregedoria)
1. O Corregedor abre o formulário da inspeção submetida.
2. Os campos de resposta do Membro ficam em modo de leitura.
3. O Corregedor audita as respostas e, se identificar inconsistências, aciona o menu lateral do campo para adicionar um **Parecer/Glosa da Corregedoria** (`observacaocorregedoria`).
4. O Corregedor aciona a ação **Devolver para Correção**.
5. O sistema altera o status da inspeção para `CORRIGIDO_DELEGACIA` e notifica o Membro.

### Fluxo Alternativo B (Retificação Assistida pelo Membro)
1. O Membro abre a inspeção que foi devolvida com o status `CORRIGIDO_DELEGACIA`.
2. O sistema exibe no rodapé da página o **Assistente de Retificação** (Bottom Sheet) expansível indicando o total de pendências encontradas pela Corregedoria.
3. O Membro utiliza o assistente para navegar sequencialmente entre as pendências (`<` e `>`):
   * O sistema realiza a rolagem automática (`scrollIntoView`) até a pergunta inconsistente.
   * O sistema aplica um destaque visual animado (`destaquePulse` de cor azul) no campo para identificação imediata.
4. O Membro realiza a correção necessária e atualiza a resposta. Os demais campos não marcados para retificação permanecem bloqueados (modo leitura).
5. Após sanar todas as pendências, o Membro clica em **Re-submeter à Corregedoria**, retornando o fluxo para o estado `VALIDACAO_CORREGEDORIA`.

---

## ⚙️ Funcionalidades e Regras de Interface (UI/UX)

### RN01 - Controle de Escrita Restrito (Retificação)
* Quando a inspeção estiver no estado `CORRIGIDO_DELEGACIA`, o Membro só possui permissão de edição nos campos que receberam um parecer da Corregedoria (`observacaocorregedoria != null`).
* Todos os demais campos e perguntas permanecem no modo leitura (`readonly`) para garantir a integridade dos dados já aprovados.

### RN02 - Assistente de Retificação (Bottom Sheet)
* **Condição de Exibição:** Visível apenas para o Membro, quando o status for `CORRIGIDO_DELEGACIA` e existirem pareceres da Corregedoria.
* **Comportamento Rebatível:** Pode ser minimizado ou expandido pelo usuário clicando na barra de cabeçalho do assistente.
* **Navegação Linear:** Botões de anterior e próximo para percorrer os índices de inconsistência de forma sequencial.
* **Foco e Destaque:**
  * O sistema foca automaticamente na respectiva pergunta ao mudar de pendência.
  * O elemento ganha a classe CSS `.destaque-campo` que dispara uma animação pulse de fundo azul claro para atrair a atenção visual do usuário.

### RN03 - Resumo de Anotações Consolidado (Ações Centralizadas)
* **Gatilho de Acesso:** Botão no cabeçalho central do formulário (ao lado de "Ver Histórico") contendo um badge (`v-chip`) indicador do total de observações na inspeção.
* **Visão Consolidada:** Exibe em formato de timeline todas as perguntas que possuem Anotação Pré-Visita (Membro) ou Parecer/Glosa (Corregedoria).
* **Exclusão Segura:**
  * Permite ao autor remover a nota diretamente clicando no ícone de lixeira.
  * **Diálogo de Confirmação:** A remoção só é efetivada após o usuário confirmar a ação em um diálogo modal (`v-dialog`) de segurança, prevenindo cliques acidentais.
  * Se o membro apagar o texto de sua anotação em um campo, a anotação correspondente é automaticamente removida.

### RN04 - Compactação Visual de Checkboxes
* Formulários contendo longas sequências de checkboxes (especialmente perguntas filhas sob tabulação lateral) possuem margens dinâmicas reduzidas (`mb-1` no wrapper e row) e densidade compacta (`density="compact"` e limite de `min-height: 28px` via deep selector CSS).
* Isso evita rolagem excessiva de tela e melhora a escaneabilidade dos dados de conformidade.

---

## 🔗 Rastreabilidade & Dependências

* **Depende de:**
  * **UC001 - Agendar Inspeção:** Necessita da inspeção previamente agendada e dos metadados de equipe e PGA.
* **Tabelas do Banco Associadas:**
  * `ncap.inspecoes`: Armazena o cabeçalho e status do fluxo.
  * `ncap.respostasinspecao`: Armazena a resposta em si (`valorresposta`), as anotações do promotor (`observacaoprevisita`) e os pareceres de auditoria (`observacaocorregedoria`).

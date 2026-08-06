# UC004 - Sincronização Automática de Unidades Policiais

Este documento descreve o processo automatizado de importação e sincronização das unidades policiais civis a partir da API externa do CNMP, executado diariamente pelo próprio sistema via job agendado.

> **Nota arquitetural:** Este não é um caso de uso iniciado por ator humano. Trata-se de um **processo de sistema** — implementado via `@Scheduled` do Spring Boot — que é executado automaticamente sem interação do usuário.

---

## 📋 Informações Gerais

* **ID/Código:** UC004
* **Nome:** Sincronização Automática de Unidades Policiais (API CNMP)
* **Tipo:** Processo de Sistema (Job Agendado)
* **Versão:** 1.0
* **Status:** Planejado
* **Mecanismo de Disparo:** Spring `@Scheduled` — execução periódica configurável (padrão: uma vez ao dia)

---

## 📌 Pré-condições & Pós-condições

### Pré-condições
1. A integração com a API do CNMP deve estar configurada (URL e credenciais disponíveis via parâmetro de configuração).
2. O sistema deve estar em execução.

### Pós-condições
1. As unidades novas encontradas na API do CNMP são inseridas na base local com status **ativo**.
2. Os dados das unidades já existentes na base local são atualizados com as informações mais recentes da API do CNMP.
3. A **descrição MPPB** (`descricaomppb`) de cada unidade é preservada caso tenha sido editada manualmente — **não é sobrescrita**.
4. Unidades que não mais existem na API do CNMP são marcadas como **inativas** (sem exclusão).
5. O resultado da execução (totais de inseridas, atualizadas e inativadas) é registrado em log de sistema.

---

## 🔄 Fluxo de Eventos

### Fluxo Principal (Execução Automática)
1. O scheduler do Spring Boot aciona o job no horário configurado (ex: diariamente às 02:00).
2. O sistema realiza a chamada à API externa do CNMP para obter a lista atual de unidades policiais civis.
3. Para cada unidade retornada pela API:
   * Se a unidade **não existe** na base local → insere com os dados da API e sem alcunha definida.
   * Se a unidade **já existe** na base local → atualiza os campos oficiais (nome CNMP, tipo, comarca, município), preservando a descrição MPPB (`descricaomppb`) já cadastrada.
4. Unidades presentes na base local mas **ausentes** da resposta da API → marcadas como **inativas**.
5. O resultado da operação é registrado em log (total inseridas, atualizadas, inativadas e eventuais erros).

### Fluxo de Exceção

#### FE01 - Falha na Comunicação com a API do CNMP
* *No passo 2*, se a API não responder ou retornar erro HTTP:
  1. O sistema registra o erro detalhado em log.
  2. Nenhuma alteração é realizada na base local (operação transacional — tudo ou nada).
  3. O sistema aguardará a próxima execução agendada para nova tentativa.

---

## ⚙️ Regras de Negócio (RN)

### RN01 - Descrição MPPB (Apelido) Editável e Preservada
* Cada unidade pode ter uma **descrição MPPB** (`descricaomppb`) editada manualmente pelos gestores do sistema.
* Esta descrição **não é sobrescrita** pela sincronização automática — persiste independentemente do valor retornado pela API.
* Caso nenhuma descrição MPPB tenha sido definida, o sistema exibe o nome oficial do CNMP em seu lugar.

### RN02 - Inativação em Vez de Exclusão
* Unidades removidas da API do CNMP não são excluídas da base local para preservar o histórico de inspeções vinculadas a elas.
* Unidades inativas **não aparecem** na seleção do formulário de agendamento de inspeção (UC001).

### RN03 - Sincronização Idempotente
* Executar o job múltiplas vezes no mesmo dia deve produzir o mesmo resultado final (sem duplicatas e sem perda de dados).

### RN04 - Configuração do Agendamento
* O horário e frequência de execução do job devem ser configuráveis via parâmetro na tabela de configuração do sistema (`administrador.configuracao`), sem necessidade de redeploy.
* Valor padrão: execução diária na madrugada (ex: `0 0 2 * * ?` em formato cron Spring).

### RN05 - Campo Tipo de Entidade (tipoentidade) e Associação de Formulário
* A API do CNMP fornece o tipo de cada unidade policial civil (mapeado para o campo `tipoentidade` na base local).
* O campo `tipoentidade` é crítico para a resolução dinâmica do formulário de inspeção correspondente (UC001). 
* Durante a criação/agendamento da inspeção, o sistema busca e vincula automaticamente o template de formulário ativo (`formtemplates.id`) que corresponda ao `tipoentidade` da unidade associada.

---

## 🔗 Rastreabilidade & Dependências

* **Casos de Uso que Dependem deste:**
  * **UC001 - Agendar Inspeção:** Exige que as unidades estejam previamente sincronizadas e ativas na base local com seus respectivos `tipoentidade` atualizados para carregar dinamicamente o formulário correto no agendamento.


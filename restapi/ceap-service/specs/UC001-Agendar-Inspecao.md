# UC001 - Agendar Inspeção

Este caso de uso descreve o fluxo para o agendamento de inspeções em delegacias/unidades policiais civis, integrando as regras da Resolução CNMP nº 279/2023.

---

## 📋 Informações Gerais

* **ID/Código:** UC001
* **Nome:** Agendar Inspeção
* **Versão:** 1.1
* **Status:** Implementado
* **Atores:** Promotor de Justiça / Corregedor / Administrador

---

## 📌 Pré-condições & Pós-condições

### Pré-condições
1. O ator deve estar autenticado no sistema.
2. A unidade policial civil (delegacia) deve estar previamente sincronizada e **ativa** na base de dados local (ver **UC004 - Sincronizar Unidades Policiais**).
3. Os membros/servidores do MPPB devem estar cadastrados no sistema corporativo (tabela `servidor`/`pessoa`).

### Pós-condições
1. O registro de inspeção é salvo no banco de dados com status `AGENDADA`.
2. O ID do usuário/servidor que criou o agendamento (`id_servidor_criador`) é gravado de forma automática com base no contexto de autenticação do Spring Security.
3. A data e hora exatas da criação (`criadoem`) são registradas automaticamente via auditoria de persistência (Hibernate `@CreationTimestamp`).
4. Um link e token de acesso exclusivo para a unidade policial preencher o formulário de autoavaliação são gerados.

---

## 🔄 Fluxo de Eventos

### Fluxo Principal (Cenário Feliz)
1. O ator acessa a tela de listagem de inspeções e solicita a criação de um novo agendamento.
2. O sistema redireciona o ator para uma nova página exclusiva de cadastro (`InspecaoFormView.vue`), contendo o formulário estruturado nas seguintes seções:
   * **Seção 1: Identificação da Unidade Policial**
     * **Unidade Policial** (selecionável).
     * **Município/Comarca** (preenchido automaticamente em modo leitura após seleção da unidade).
   * **Seção 2: Procedimento Administrativo (PGA)**
     * **Número do PGA** (opcional) e **Link do PGA** (opcional).
   * **Seção 3: Detalhes da Inspeção & Conformidade**
     * **Data da Visita** (seleção por calendário).
     * **Semestre de Referência** (determinado ou preenchido conforme a data de agendamento/visita).
     * **Tipo de Visita** (Presencial / Virtual).
     * **Data Limite de Resposta** (pré-preenchida de acordo com a regra **RN02**, editável pelo usuário).
   * **Seção 4: Responsáveis & Equipe de Inspeção**
     * **Responsável Principal** (Delegado / Comandante) e **E-mail do responsável**.
     * **Promotor Responsável** (Código do promotor).
     * **Equipe de Inspeção:** Multiseleção de Membros do MPPB (IDs de servidores pesquisados do banco de dados).
3. O ator seleciona os dados e preenche os campos requeridos.
4. O ator aciona a ação de salvar o agendamento.
5. O sistema executa as validações de negócios (**RN01**, **RN02** e **RN03**).
6. O sistema persiste a inspeção, gera o token de acesso e redireciona o ator de volta à tela de listagem com uma mensagem de sucesso.

### Fluxos Alternativos e de Exceção

#### FA01 - Seleção de Visita Virtual (Resolução 279/2023)
* *No passo 2*, se o ator selecionar o tipo de visita **Virtual**:
  1. O sistema torna obrigatória a seleção de uma **Justificativa Remota** (conforme artigo correspondente da resolução):
     * *Opção I:* Presença física impossibilitada.
     * *Opção II:* Unidade já fiscalizada em PGA específico.
  2. O sistema exibe um campo de texto textarea ("Detalhamento da Justificativa") que passa a ser obrigatório (limitado a 2000 caracteres).
  3. O fluxo retorna ao passo 3.

---

## ⚙️ Regras de Negócio (RN)

### RN01 - Restrição de Semestre e Tipo de Visita (Res. 279/2023)
A validação do tipo de visita deve ser condicionada ao semestre da data selecionada:
* **1º Semestre (Visitas de Janeiro a Abril):**
  * O tipo de visita é restrito exclusivamente a **Presencial**.
  * Caso o usuário tente forçar outro modo, o sistema gera erro de validação.
  * Período correspondente aos dados da unidade: Julho a Dezembro do ano anterior.
* **2º Semestre (Visitas de Julho a Outubro):**
  * O usuário pode escolher entre **Presencial** ou **Virtual**.
  * A escolha de "Virtual" engatilha a obrigatoriedade da justificativa (**FA01**).
  * Período correspondente aos dados da unidade: Janeiro a Junho do ano corrente.

### RN02 - Data Limite de Resposta (Padrão D+15)
* Ao selecionar a data da visita/agendamento, o sistema calcula automaticamente: `Data Limite = Data da Visita + 15 dias`.
* Este parâmetro de 15 dias é configurável globalmente.
* O valor calculado é preenchido na tela por padrão, mas permanece editável para que o ator possa ajustar a data limite manualmente se necessário.

### RN03 - Seleção e Persistência de Membros (Servidores)
* O formulário deve disponibilizar uma busca autocomplete de servidores.
* O sistema buscará na tabela `public.servidor` (usando o padrão já implementado na busca de ministrantes em `cursos-service`).
* **Validação de Cargo/Vínculo:** Devem ser listados apenas membros do Ministério Público (tipo `membro`) e que estejam com status **ativo**. Servidores gerais ou inativos devem ser descartados na busca do autocomplete.
* No banco de dados, o agendamento salvará a lista de IDs de servidores participantes na equipe da inspeção.

### RN04 - Impedimento de Duplicidade de Agendamento por Semestre
* Antes de confirmar um novo agendamento, o sistema deve validar se já existe uma inspeção cadastrada para a mesma **Unidade Policial** no mesmo **Semestre de Referência**.
* Caso já exista uma inspeção ativa para aquela unidade no semestre (independentemente do status atual, exceto se cancelada/inativada), o sistema deve impedir a criação do novo agendamento, exibindo uma mensagem de alerta: *"Já existe um agendamento de inspeção em andamento ou concluído para esta unidade no semestre selecionado."*

---

## 🔗 Rastreabilidade & Dependências

* **Pré-requisito de Dados (entrada):**
  * **UC004 - Sincronizar Unidades Policiais:** As unidades disponíveis no formulário de agendamento são provenientes da base local, que deve ser populada/atualizada via sincronização com a API do CNMP. Unidades inativas não aparecem na seleção.

* **Casos de Uso Relacionados / Dependentes (saída):**
  * **UC002 - Responder Autoavaliação da Unidade:** Necessita que o agendamento (UC001) tenha sido concluído com sucesso para que o token/link seja enviado ao e-mail do responsável cadastrado e os dados de autoavaliação sejam salvos vinculados àquela inspeção.
  * **UC003 - Realizar Inspeção:** Depende do prévio agendamento em UC001 para obter os dados do PGA e equipe inspetora.iação sejam salvos vinculados àquela inspeção.
  * **UC003 - Realizar Inspeção:** Depende do prévio agendamento em UC001 para obter os dados do PGA e equipe inspetora.

# Especificação de Funcionalidade: Mensagens e Avisos Institucionais (`mobile-service`)

**Branch da Feature**: `feat/mensagens-avisos`  
**Criado em**: 2026-11-18  
**Status**: Rascunho (Draft)  
**Subprojeto**: `restapi/mobile-service`  
**Constituição Aplicável**: `spring-boot/.specify/memory/constitution.md`

## Cenários de Usuário e Testes *(obrigatório)*

### História de Usuário 1 - Envio de Mensagens e Avisos por Autoridade Designada (Priority: P1)

Como PGJ ou autoridade designada (usuário autenticado com perfil/permissão genérica de gestão do aplicativo no sistema `autorizacao`), quero enviar mensagens e avisos institucionais nas modalidades **broadcast**, **multicast** ou **unicast**, com suporte a anexos e prazo de expiração, para comunicar servidores e membros do MPPB diretamente pelo aplicativo.

**Por que esta prioridade**: É o núcleo da funcionalidade no backend: autenticação por perfil/permissão, resolução de destinatários nas tabelas de RH, persistência da mensagem, geração dos registros individuais de rastreio e disparo de push FCM.

**Teste Independente**: Pode ser testado de forma isolada via endpoints REST protegidos do `mobile-service` (com usuário autorizado e usuário sem permissão para validar HTTP 403), verificando a criação da mensagem, a expansão correta de destinatários (broadcast, multicast, unicast) e o enfileiramento/envio FCM.

**Critérios de Aceitação**:

1. **Dado** um usuário autenticado com a permissão adequada em `AcessoDataResponseDto` (`SimpleGrantedAuthority`), **Quando** ele envia uma mensagem em modalidade `BROADCAST`, **Então** o sistema cria a mensagem e gera registros de destinatário para todos os servidores e membros ativos (`servidor.ativo = 'A'` e `excluido = false`).
2. **Dado** um usuário autenticado sem a permissão de envio, **Quando** ele tenta criar, editar, cancelar ou consultar relatórios de envio de mensagens, **Então** o sistema nega o acesso com HTTP 403 Forbidden.
3. **Dado** um envio `MULTICAST` por grupo predefinido (ex.: somente Membros, somente Servidores, por Órgão/Setor ou por Cargo/Função) ou grupo personalizado, **Quando** o envio é processado, **Então** apenas os destinatários ativos pertencentes aos critérios/grupos selecionados recebem a mensagem (sem duplicidade caso um usuário pertença a mais de um critério selecionado).
4. **Dado** um envio `UNICAST`, **Quando** o remetente informa uma ou mais matrículas específicas selecionadas na busca de servidores/membros, **Então** apenas esses destinatários recebem a mensagem.
5. **Dado** uma mensagem apenas de texto, apenas de mídia/anexo (somente foto, somente vídeo, somente áudio/mensagem de voz ou somente documento) ou combinada, com ou sem `dataExpiracao`, **Quando** o remetente realiza o upload/envio, **Então** o sistema valida que há pelo menos texto ou um anexo válido, verifica tipo MIME, extensão permitida e tamanho máximo por categoria, armazena de forma segura e persiste a `dataExpiracao` (usando rótulo padrão no push caso `titulo` seja omitido).

---

### História de Usuário 2 - Recebimento, Push FCM, Entrega e Confirmação de Leitura (Priority: P1)

Como servidor ou membro do MPPB (destinatário), quero receber notificações push (FCM) e consultar minhas mensagens e avisos em uma caixa de entrada própria no aplicativo, tendo meu recebimento e minha leitura registrados com data e hora.

**Por que esta prioridade**: Completa o ciclo ponta a ponta exigido pela instituição (entrega garantida na caixa de entrada + notificação push + rastreabilidade de entrega e leitura).

**Teste Independente**: Pode ser testado autenticando-se como destinatário, listando a caixa de entrada, sincronizando/confirmando entrega, abrindo o detalhe da mensagem para registrar leitura e verificando a atualização dos timestamps `dataEntrega` e `dataLeitura`.

**Critérios de Aceitação**:

1. **Dado** uma mensagem enviada para o destinatário, **Quando** o dispositivo recebe o push FCM ou o app sincroniza a caixa de entrada, **Então** o status do destinatário evolui para `ENTREGUE` com registro de `dataEntrega` (idempotente: chamadas repetidas não sobrescrevem a primeira `dataEntrega`).
2. **Dado** uma mensagem na caixa de entrada do destinatário, **Quando** o destinatário abre a mensagem (ou confirma leitura), **Então** o status evolui para `LIDO` com registro de `dataLeitura` (e `dataEntrega`, caso ainda estivesse pendente), de forma idempotente.
3. **Dado** uma mensagem cuja `dataExpiracao` já passou ou que foi arquivada pelo destinatário, **Quando** o destinatário lista sua caixa de entrada ativa, **Então** mensagens expiradas/arquivadas não aparecem na caixa principal (podendo ser filtradas em histórico/arquivadas, quando aplicável).
4. **Dado** um usuário que **não** é destinatário de uma mensagem `UNICAST` ou `MULTICAST`, **Quando** ele tenta acessar o detalhe ou o anexo dessa mensagem pelo ID, **Então** o sistema nega o acesso (HTTP 403/404) — Princípio IV da constituição.

---

### História de Usuário 3 - Painel de Acompanhamento do Remetente (Quem e Quando Recebeu/Leu) (Priority: P2)

Como PGJ ou autoridade designada, quero consultar a lista de mensagens que enviei com indicadores consolidados (total de destinatários, entregues, lidos e pendentes) e o detalhamento individual de **quem** recebeu/leu e **quando**, para auditar o alcance das comunicações.

**Por que esta prioridade**: Atende diretamente ao requisito central do negócio ("saber quando foi entregue e quem e quando foi lido"), agregando valor de auditoria sobre os dados já persistidos na P1.

**Teste Independente**: Pode ser testado consultando os endpoints de resumo e detalhamento paginado de destinatários após simular entregas e leituras de diferentes usuários.

**Critérios de Aceitação**:

1. **Dado** uma mensagem enviada, **Quando** o remetente autorizado consulta o resumo da mensagem, **Então** o sistema retorna contagens exatas de `totalDestinatarios`, `totalEntregues`, `totalLidos` e `totalPendentes`.
2. **Dado** o detalhamento de destinatários de uma mensagem, **Quando** o remetente filtra por status (`PENDENTE`, `ENTREGUE`, `LIDO`) ou busca por nome/matrícula/setor, **Então** o sistema retorna lista paginada contendo identificação do destinatário, `status`, `dataEntrega` e `dataLeitura`.

---

### História de Usuário 4 - Gestão de Grupos Predefinidos (RH) e Grupos Personalizados (Priority: P2)

Como PGJ ou autoridade designada, quero selecionar grupos predefinidos baseados na estrutura de RH (Membros, Servidores, Órgãos/Setores, Cargos/Funções) e criar/gerenciar grupos personalizados de destinatários, para agilizar envios recorrentes em modalidade `MULTICAST`.

**Por que esta prioridade**: Dá flexibilidade operacional para comunicados direcionados sem depender de seleção manual usuário a usuário a cada envio.

**Teste Independente**: Pode ser testado listando os grupos dinâmicos de RH (consultando `servidor`, `recursoshumanos.funcao`, `recursoshumanos.tipocargo`, `setor`, `orgao`) e realizando CRUD de grupos personalizados com adição/remoção de matrículas.

**Critérios de Aceitação**:

1. **Dado** o catálogo de grupos dinâmicos de RH, **Quando** o remetente consulta os grupos disponíveis, **Então** o sistema retorna opções derivadas do RH (`Membro`/`Servidor` via `recursoshumanos.tipocargo`, órgãos via `orgao`, setores via `setor`, cargos via `recursoshumanos.funcao`) com contagem estimada de ativos.
2. **Dado** um remetente autorizado, **Quando** ele cria, edita, adiciona/remove membros ou inativa um grupo personalizado, **Então** o sistema persiste o grupo em tabelas próprias do `mobile-service` e o disponibiliza para futuros envios `MULTICAST`.

### Casos de Borda (Edge Cases)

- **Usuário sem dispositivo FCM registrado ou com push desativado**: A mensagem **deve** continuar disponível na caixa de entrada do app; o status de push não impede o recebimento in-app.
- **Múltiplos dispositivos do mesmo usuário**: Se o usuário possui Android e iOS registrados em `dispositivos_fcm`, o push é enviado a todos os dispositivos ativos, mas a entrega/leitura é contabilizada uma única vez por destinatário (`matricula`), mantendo o primeiro timestamp registrado.
- **Mudança de lotação/inativação após o envio**: A lista de destinatários (`mensagem_destinatario`) é materializada no momento do envio (snapshot), garantindo rastreabilidade histórica fiel mesmo que o servidor mude de setor depois.
- **Anexos inválidos ou acima do limite**: Rejeitar arquivos fora da lista de MIME types permitidos ou acima do limite configurado, sem criar registro órfão.
- **Falha parcial no envio FCM**: Falhas transitórias de FCM devem ser registradas para diagnóstico sem reverter a persistência da mensagem nem duplicar destinatários.

## Requisitos *(obrigatório)*

### Requisitos Funcionais

- **FR-001**: O sistema **DEVE** restringir criação, envio, cancelamento/arquivamento administrativo, gestão de grupos personalizados e consulta de relatórios de entrega/leitura a usuários autenticados que possuam a permissão/authority genérica configurada no sistema `autorizacao`.
- **FR-002**: O sistema **DEVE** suportar 3 modalidades de endereçamento: `BROADCAST` (todos os servidores/membros ativos), `MULTICAST` (grupos dinâmicos de RH e/ou grupos personalizados) e `UNICAST` (matrículas específicas).
- **FR-003**: O sistema **DEVE** resolver grupos dinâmicos de RH utilizando as tabelas existentes (`servidor`, `pessoa`, `recursoshumanos.funcao`, `recursoshumanos.tipocargo`, `setor`, `orgao`), considerando apenas vínculos ativos (`servidor.ativo = 'A'`, `excluido = false`) e deduplicando destinatários por `matricula`.
- **FR-004**: O sistema **DEVE** permitir CRUD de grupos personalizados (`grupo_mensagem` e `grupo_mensagem_membro`) vinculados à `matricula`.
- **FR-005**: O sistema **DEVE** persistir o estado individual por destinatário (`PENDENTE`, `ENTREGUE`, `LIDO`) com `data_entrega` e `data_leitura` imutáveis após o primeiro registro (idempotência).
- **FR-006**: O sistema **DEVE** integrar-se à infraestrutura existente de `DispositivoFcmService` / `FcmSenderService` para disparar notificações push com payload contendo identificador da mensagem e rota de destino no app.
- **FR-007**: O sistema **DEVE** suportar anexos e mídias ricas agrupados por categoria, armazenados fora do banco relacional (em storage de arquivos com referência persistida), com validação rigorosa de MIME type/extensão, nome seguro, limites configuráveis por categoria, metadados de exibição inline (como duração para áudio/vídeo e thumbnail/preview para imagem/vídeo quando aplicável) e download/streaming autenticado (com suporte a HTTP `Range` para execução direta de áudio e vídeo na conversa) restrito ao remetente autorizado ou destinatário da mensagem:
  - **Imagens/Fotos** (até 10 MB): JPEG, PNG, WebP, HEIC;
  - **Vídeos** (até 50 MB): MP4, MOV, WebM;
  - **Áudio / Mensagem de Voz** (até 15 MB): M4A/AAC, OGG/Opus, MP3, WAV (incluindo metadados de duração em segundos);
  - **Documentos e Texto** (até 25 MB): PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, ODT/ODS/ODP, TXT, CSV, MD (Markdown).
- **FR-008**: O sistema **DEVE** garantir que mensagens e avisos sejam **estritamente unilaterais** (apenas autoridade remetente → destinatários, sem canal de resposta/chat) e direcionados **exclusivamente a usuários institucionais autenticados** (servidores e membros com matrícula ativa); usuários públicos sem login **nunca** recebem notificações ou listagens de avisos, e o logout deve desativar/desvincular o dispositivo FCM para evitar entregas indevidas.
- **FR-009**: O sistema **DEVE** suportar níveis de prioridade (`NORMAL`, `IMPORTANTE`, `URGENTE`), confirmação opcional de ciência explícita (`exige_ciencia` + `data_ciencia` registrada pelo destinatário), prévia de contagem de destinatários antes do envio, agendamento de publicação (`data_agendamento`) e `data_expiracao`/arquivamento (pelo destinatário ou encerramento pelo remetente).
- **FR-010**: Toda a implementação no `mobile-service` **DEVE** seguir o pacote `mppb.restapi.mobile.feature.mensagem` (ou `mensagens`) conforme o Princípio II da constituição (`controller`, `dto`, `entity`/`model`, `enums`, `repository`, `service`, `job` para envios agendados).

### Entidades Principais

- **MensagemAviso**: Representa o comunicado unilateral enviado (`id`, `titulo`, `conteudo`, `prioridade` [`NORMAL`, `IMPORTANTE`, `URGENTE`], `modalidade` [`BROADCAST`, `MULTICAST`, `UNICAST`], `exigeCiencia`, `remetenteMatricula`, `statusPublicacao` [`AGENDADA`, `ENVIADA`, `CANCELADA`], `dataAgendamento`, `dataEnvio`, `dataExpiracao`, `arquivada`).
- **MensagemDestinatario**: Representa a entrega/leitura/ciência individual (`id`, `mensagemId`, `destinatarioMatricula`, `status` [`PENDENTE`, `ENTREGUE`, `LIDO`], `dataEntrega`, `dataLeitura`, `dataCiencia`, `arquivadaDestinatario`).
- **MensagemAnexo**: Representa arquivo anexado à mensagem (`id`, `mensagemId`, `nomeOriginal`, `mimeType`, `tamanhoBytes`, `caminhoStorage`/`referencia`).
- **GrupoMensagem / GrupoMensagemMembro**: Representa grupos personalizados criados pelos remetentes autorizados (`id`, `nome`, `descricao`, `ativo`, `criadoPorMatricula`, lista de `matriculas`).

## Critérios de Sucesso *(obrigatório)*

### Resultados Mensuráveis

- **SC-001**: Um envio `BROADCAST` para toda a base ativa materializa destinatários deduplicados e inicia o disparo FCM assíncrono/em lote sem travar a requisição HTTP do remetente.
- **SC-002**: 100% dos endpoints administrativos negam acesso (HTTP 403) a usuários sem a authority exigida em testes automatizados (cenários positivos e negativos).
- **SC-003**: Endpoints de destinatário garantem isolamento por matrícula autenticada (nenhum usuário acessa mensagens ou anexos de outro usuário).
- **SC-004**: Confirmações repetidas de entrega ou leitura preservam exatamente o primeiro timestamp registrado.

## Premissas

- A autorização continuará utilizando o provedor existente em `restapi-common` (`AcessoDataResponseDto` / `SimpleGrantedAuthority`), sendo cadastrada no sistema `autorizacao` a permissão/perfil genérico que habilita o painel de envio e gestão.
- O envio de push utilizará o Firebase Cloud Messaging já configurado em `feature/notificacao` (`FirebaseConfig`, `DispositivoFcmRepository`, `FcmSenderService`).
- As tabelas de RH (`servidor`, `pessoa`, `recursoshumanos.funcao`, `recursoshumanos.tipocargo`, `setor`, `orgao`) são somente leitura a partir do `mobile-service`.


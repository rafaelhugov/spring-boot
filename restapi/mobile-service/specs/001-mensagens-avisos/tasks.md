---
description: "Lista de tarefas para implementação de Mensagens e Avisos Institucionais no mobile-service"
---

# Tarefas: Mensagens e Avisos Institucionais (`mobile-service`)

**Entrada**: Documentos de design em `restapi/mobile-service/specs/001-mensagens-avisos/` (`spec.md`, `plan.md`, `data-model.md`, `contracts/api-mensagens.md`)  
**Constituição**: `spring-boot/.specify/memory/constitution.md`

## Formato: `[ID] [P?] [Story] Descrição`
- **[P]**: Pode executar em paralelo (arquivos distintos, sem dependência bloqueante)
- **[Story]**: História de usuário correspondente (`US1`, `US2`, `US3`, `US4`)

---

## Fase 1: Setup e Infraestrutura SQL

- [x] T001 Criar script SQL de tabelas e índices (`mensagem_aviso`, `mensagem_destinatario`, `mensagem_anexo`, `grupo_mensagem`, `grupo_mensagem_membro`) em `restapi/mobile-service/src/main/resources/sql/2026-11-18_mensagens_avisos.sql` conforme `data-model.md`
- [x] T002 Configurar propriedades de armazenamento de mídias/anexos e limites por categoria (`mobile.mensagens.*`) em `restapi/mobile-service/src/main/resources/application.yml`

---

## Fase 2: Fundação (Pré-requisitos Bloqueantes)

- [x] T003 [P] Criar enums de domínio (`ModalidadeEnvio`, `PrioridadeMensagem`, `StatusPublicacao`, `StatusDestinatario`, `CategoriaAnexo`) em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/enums/`
- [x] T004 [P] Criar entidades JPA (`MensagemAviso`, `MensagemDestinatario`, `MensagemAnexo`, `GrupoMensagem`, `GrupoMensagemMembro`) em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/entity/`
- [x] T005 [P] Criar repositórios JPA (`MensagemAvisoRepository`, `MensagemDestinatarioRepository`, `MensagemAnexoRepository`, `GrupoMensagemRepository`, `GrupoMensagemMembroRepository`) em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/repository/`
- [x] T006 Criar `RhDestinatarioRepository` (consultas somente leitura via `JdbcTemplate` em `servidor`, `pessoa`, `recursoshumanos.funcao`, `recursoshumanos.tipocargo`, `setor`, `orgao`) em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/repository/RhDestinatarioRepository.java`
- [x] T007 Criar `MensagemStorageService` (validação estrita de MIME/extensão, nome UUID seguro, limites por categoria: Imagem 10 MB, Áudio 15 MB, Documento 25 MB, Vídeo 50 MB, e streaming com HTTP `Range`) em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/service/MensagemStorageService.java`

---

## Fase 3: História de Usuário 1 — Envio de Mensagens e Avisos por Autoridade Designada (P1)

**Objetivo**: Permitir envio `BROADCAST`, `MULTICAST` e `UNICAST` (apenas texto, apenas mídia/áudio ou combinados), com prévia de alcance, agendamento, expiração, push FCM e proteção por `@PreAuthorize`.

- [x] T008 [P] [US1] Criar DTOs de requisição/resposta de envio, prévia de alcance e anexo em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/dto/`
- [x] T009 [US1] Implementar `MensagemPushService` integrando `DispositivoFcmService` / `FcmSenderService` para disparo em lote apenas a dispositivos ativos das matrículas destinatárias (com fallback de título para avisos só de mídia) em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/service/MensagemPushService.java`
- [x] T010 [US1] Implementar `MensagemEnvioService` (validação texto ou anexo obrigatório, prévia de contagem, snapshot deduplicado de destinatários, envio imediato/agendado, cancelamento e encerramento) em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/service/MensagemEnvioService.java`
- [x] T011 [P] [US1] Implementar `MensagemAgendadaScheduler` em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/job/MensagemAgendadaScheduler.java`
- [x] T012 [US1] Implementar endpoints administrativos (`POST /api/mensagens/admin/previa-alcance`, `POST /api/mensagens/admin/anexos`, `POST /api/mensagens/admin`, `PATCH /api/mensagens/admin/{id}/cancelar`, `PATCH /api/mensagens/admin/{id}/arquivar`) com `@PreAuthorize` em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/controller/MensagemRemetenteController.java`
- [x] T013 [US1] Criar testes unitários e de segurança (cenários positivos + HTTP 403 negativo + validação de mídia/limites + deduplicação) em `restapi/mobile-service/src/test/java/mppb/restapi/mobile/feature/mensagem/MensagemEnvioTest.java`

---

## Fase 4: História de Usuário 2 — Recebimento, Entrega, Leitura, Ciência e Streaming (P1)

**Objetivo**: Caixa de entrada exclusiva para usuários autenticados, confirmação idempotente de entrega/leitura/ciência, arquivamento e download/streaming autenticado de anexos.

- [x] T014 [P] [US2] Criar DTOs de caixa de entrada e detalhe do destinatário em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/dto/`
- [x] T015 [US2] Implementar `MensagemDestinatarioService` (listagem filtrada ignorando expiradas, confirmação em lote `ENTREGUE`, abertura com `LIDO` idempotente, `CIENCIA` explícita, arquivamento e verificação de vínculo por matrícula) em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/service/MensagemDestinatarioService.java`
- [x] T016 [US2] Implementar `MensagemDestinatarioController` (`GET /recebidas`, `POST /recebidas/confirmar-entrega`, `GET /recebidas/{id}`, `POST /recebidas/{id}/ciencia`, `PATCH /recebidas/{id}/arquivar`, `GET /{mensagemId}/anexos/{anexoId}/conteudo` com HTTP `Range`) em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/controller/MensagemDestinatarioController.java`
- [x] T017 [US2] Criar testes de isolamento por matrícula (IDOR) e idempotência de timestamps (`dataEntrega`, `dataLeitura`, `dataCiencia`) em `restapi/mobile-service/src/test/java/mppb/restapi/mobile/feature/mensagem/MensagemDestinatarioTest.java`

---

## Fase 5: História de Usuário 3 — Painel de Acompanhamento do Remetente (P2)

**Objetivo**: Permitir ao remetente autorizado auditar totais consolidados e consultar paginadamente quem recebeu, leu e deu ciência e quando.

- [x] T018 [P] [US3] Criar DTOs de resumo de enviadas e acompanhamento paginado de destinatários em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/dto/`
- [x] T019 [US3] Adicionar consultas de contagem consolidada e listagem paginada filtrável (`status`, `busca`) em `MensagemEnvioService` e endpoints (`GET /api/mensagens/admin/enviadas`, `GET /api/mensagens/admin/{id}/acompanhamento`) em `MensagemRemetenteController.java`
- [x] T020 [US3] Criar testes de agregação e filtros do acompanhamento em `restapi/mobile-service/src/test/java/mppb/restapi/mobile/feature/mensagem/MensagemAcompanhamentoTest.java`

---

## Fase 6: História de Usuário 4 — Grupos Predefinidos (RH) e Grupos Personalizados (P2)

**Objetivo**: Expor catálogo dinâmico de grupos de RH e CRUD de grupos personalizados + busca de destinatários.

- [x] T021 [P] [US4] Criar DTOs de grupos predefinidos RH, grupos personalizados e busca de destinatários em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/dto/`
- [x] T022 [US4] Implementar `GrupoMensagemService` em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/service/GrupoMensagemService.java`
- [x] T023 [US4] Implementar `GrupoMensagemController` (`/api/mensagens/admin/grupos/predefinidos`, `/personalizados`, `/destinatarios/buscar`) com `@PreAuthorize` em `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/controller/GrupoMensagemController.java`
- [x] T024 [US4] Criar testes de CRUD de grupos personalizados e busca RH em `restapi/mobile-service/src/test/java/mppb/restapi/mobile/feature/mensagem/GrupoMensagemTest.java`

---

## Fase 7: Polimento e Verificação Final

- [x] T025 Verificar documentação OpenAPI gerada para todos os novos endpoints e executar `mvn test -pl restapi/mobile-service` a partir de `spring-boot/`


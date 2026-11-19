# Plano de Implementação: Mensagens e Avisos Institucionais (`mobile-service`)

**Branch**: `feat/mensagens-avisos` | **Data**: 2026-11-18 | **Spec**: [spec.md](./spec.md)  
**Subprojeto**: `restapi/mobile-service`  
**Constituição Aplicável**: `spring-boot/.specify/memory/constitution.md`

## Resumo

Implementar no `mobile-service` a funcionalidade completa de **Mensagens e Avisos Institucionais** unilaterais para usuários autenticados (servidores e membros), com envio em **Broadcast**, **Multicast** (grupos dinâmicos de RH e grupos personalizados) e **Unicast**, suporte a **mídias ricas** (fotos, vídeos, áudios/mensagens de voz e documentos), **prioridade**, **confirmação opcional de ciência**, **agendamento**, **expiração/arquivamento**, **push FCM** e **rastreamento idempotente** de entrega, leitura e ciência.

## Contexto Técnico

- **Linguagem/Versão**: Java (conforme `restapi-parent` / `mobile-service`) + Spring Boot
- **Dependências Principais**: `restapi-common`, `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui`, `postgresql`, `firebase-admin` (via infraestrutura existente em `feature/notificacao`).
- **Armazenamento**:
  - **PostgreSQL**: tabelas próprias para mensagens, destinatários, metadados de anexos e grupos personalizados; leitura das tabelas existentes de RH (`servidor`, `pessoa`, `recursoshumanos.funcao`, `recursoshumanos.tipocargo`, `setor`, `orgao`) e autorização (`autorizacao.usuario`).
  - **Storage de Arquivos (fora do banco)**: diretório configurável por propriedade (`mobile.mensagens.storage-path`) para armazenar mídias/anexos com nomes UUID seguros e streaming autenticado com suporte a HTTP `Range`.
- **Testes**: JUnit 5 + Spring Boot Test (`mvn test -pl restapi/mobile-service` a partir de `spring-boot/`).
- **Plataforma Alvo**: API REST consumida pelo `app_mppb_flutter`.

## Verificação da Constituição (`spring-boot/.specify/memory/constitution.md`)

- **Princípio I (Constituição única e isolamento)**: Spec e código restritos a `restapi/mobile-service` e constituição central em `spring-boot/.specify/memory/constitution.md`. Nenhum arquivo de `restapi-common` precisa ser alterado.
- **Princípio II (Organização por funcionalidade)**: Pacote `mppb.restapi.mobile.feature.mensagem` estruturado em `controller/`, `converter/`, `dto/`, `entity/`, `enums/`, `job/`, `repository/`, `service/`.
- **Princípio III (Maven)**: Nenhuma nova dependência Maven necessária; reutiliza JPA, JdbcTemplate, Security, Validation, OpenAPI e FCM já presentes no `mobile-service`.
- **Princípio IV (Segurança)**:
  - Endpoints administrativos/remetente protegidos por `@PreAuthorize("hasAuthority('androidmppb.mensagens.admin')")` (parametrizável via `mobile.mensagens.authority-admin`, vinculada ao perfil `Administrador` do sistema `Aplicativo Android MPPB` junto com `androidmppb.acesso`, sem impactar regras de Membro/Servidor que vêm do RH `tipoCargo`).
  - Endpoints de destinatário validam a matrícula autenticada (`AutenticacaoAtual`), impedindo acesso a mensagens ou anexos de terceiros (IDOR).
  - Upload valida MIME type, extensão, nome seguro e limites por categoria (Imagem 10 MB, Áudio 15 MB, Documento 25 MB, Vídeo 50 MB).
- **Princípio VI & VII (Contratos e pt-BR)**: DTOs explícitos (sem expor entidades JPA), documentação OpenAPI e textos/documentação em pt-BR.
- **Princípio VIII & IX (Incremental e Testes)**: Testes unitários e de integração cobrindo cenários positivos e negativos (403, isolamento por matrícula, idempotência de entrega/leitura/ciência, deduplicação de destinatários).

## Estrutura do Projeto

### Documentação (`restapi/mobile-service/specs/001-mensagens-avisos/`)
- `spec.md` — Requisitos funcionais e critérios de aceitação
- `plan.md` — Este plano técnico
- `data-model.md` — Modelagem SQL PostgreSQL, índices e consultas de RH
- `contracts/api-mensagens.md` — Contratos REST de remetente, destinatário, grupos e mídias
- `tasks.md` — A ser gerado pelo `speckit-tasks`

### Código-Fonte (`restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/`)
```text
feature/mensagem/
├── controller/
│   ├── MensagemDestinatarioController.java   # Caixa de entrada, entrega, leitura, ciência, arquivar, download/stream
│   ├── MensagemRemetenteController.java      # Prévia, envio/agendamento, enviadas, acompanhamento, encerramento
│   └── GrupoMensagemController.java          # Catálogo de grupos RH + CRUD de grupos personalizados + busca RH
├── dto/                                      # Requests e Responses explícitos (sem expor entities)
├── entity/                                   # MensagemAviso, MensagemDestinatario, MensagemAnexo, GrupoMensagem, GrupoMensagemMembro
├── enums/                                    # ModalidadeEnvio, PrioridadeMensagem, StatusPublicacao, StatusDestinatario, CategoriaAnexo
├── job/                                      # MensagemAgendadaScheduler (processa mensagens agendadas)
├── repository/                               # JPA Repositories + RhDestinatarioRepository (JdbcTemplate nas tabelas RH)
└── service/                                  # MensagemEnvioService, MensagemDestinatarioService, MensagemStorageService, GrupoMensagemService, MensagemPushService
```

## Decisões de Arquitetura

1. **Snapshot de Destinatários no Envio**: Ao publicar uma mensagem (imediata ou via scheduler no horário agendado), o `RhDestinatarioRepository` resolve as matrículas ativas (`servidor.ativo = 'A'` e `excluido = false`) com `DISTINCT` e grava `MensagemDestinatario` em lote. Isso garante auditoria histórica fiel mesmo se um servidor mudar de lotação depois.
2. **Rastreio Idempotente**:
   - `ENTREGUE`: só preenche `data_entrega` se ainda for `null` e status for `PENDENTE`.
   - `LIDO`: preenche `data_leitura` (se `null`) e garante `data_entrega` (se `null`).
   - `CIENCIA`: se `exige_ciencia = true`, preenche `data_ciencia` (se `null`).
3. **Push FCM Exclusivo para Autenticados**: Reutiliza `DispositivoFcmService` / `FcmSenderService` filtrando apenas dispositivos ativos vinculados às matrículas destinatárias. No logout do app, o token é desativado.
4. **Mídias e Streaming Autenticado**: Arquivos salvos em disco/volume fora do banco (`UUID` + extensão validada), servidos por `StreamingResponseBody` / `ResourceRegion` com suporte a cabeçalho `Range` (`206 Partial Content`) para áudio e vídeo no Flutter.


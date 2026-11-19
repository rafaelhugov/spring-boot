# Contratos de API REST: Mensagens e Avisos (`mobile-service`)

**Base Path**: `/api/mensagens` (ajustar prefixo caso `server.servlet.context-path` exista no `application.yml`)  
**Autenticação**: Obrigatória em todos os endpoints (usuário público sem login não tem acesso).

## 1. Endpoints do Destinatário (Qualquer Usuário Institucional Autenticado)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/mensagens/recebidas` | Lista paginada da caixa de entrada (`filtro`: `TODAS`, `NAO_LIDAS`, `ARQUIVADAS`; ignora expiradas na caixa ativa). Retorna também `totalNaoLidas`. |
| `POST` | `/api/mensagens/recebidas/confirmar-entrega` | Confirma entrega em lote (IDs recebidos via sync/push) — idempotente. |
| `GET` | `/api/mensagens/recebidas/{id}` | Detalhe da mensagem (valida se a matrícula é destinatária). Registra `ENTREGUE` + `LIDO` automaticamente (idempotente). |
| `POST` | `/api/mensagens/recebidas/{id}/ciencia` | Registra confirmação explícita de ciência (`dataCiencia`) quando `exigeCiencia = true`. |
| `PATCH` | `/api/mensagens/recebidas/{id}/arquivar` | Arquiva ou desarquiva a mensagem na caixa do destinatário. |
| `GET` | `/api/mensagens/{mensagemId}/anexos/{anexoId}/conteudo` | Download ou streaming autenticado (suporte a `Range` para áudio/vídeo), validando que o usuário é destinatário ou remetente autorizado. |
| `GET` | `/api/mensagens/{mensagemId}/anexos/{anexoId}/thumbnail` | Retorna miniatura/preview autenticada de imagem ou vídeo para renderização rápida na conversa. |

## 2. Endpoints do Remetente / Gestor (`@PreAuthorize("hasAuthority('androidmppb.mensagens.admin')")`)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/mensagens/admin/previa-alcance` | Recebe `modalidade` + critérios/grupos/matrículas e retorna `{ "totalEstimado": 142 }`. |
| `POST` | `/api/mensagens/admin/anexos` | Upload `multipart/form-data` de mídia/anexo (valida categoria, MIME e limite de tamanho), retornando `anexoId` temporário/vinculável ou envio direto multipart. |
| `POST` | `/api/mensagens/admin` | Cria e envia (ou agenda se `dataAgendamento` futura) a mensagem/aviso. |
| `GET` | `/api/mensagens/admin/enviadas` | Lista paginada das mensagens enviadas/agendadas com contadores (`totalDestinatarios`, `totalEntregues`, `totalLidos`, `totalCientes`, `totalPendentes`). |
| `GET` | `/api/mensagens/admin/{id}/acompanhamento` | Detalhe consolidado + lista paginada de destinatários (`status`, `busca` por nome/matrícula/setor, `dataEntrega`, `dataLeitura`, `dataCiencia`). |
| `PATCH` | `/api/mensagens/admin/{id}/cancelar` | Cancela mensagem ainda `AGENDADA`. |
| `PATCH` | `/api/mensagens/admin/{id}/arquivar` | Encerra/arquiva mensagem enviada. |

## 3. Endpoints de Grupos e Busca RH (`@PreAuthorize("hasAuthority('androidmppb.mensagens.admin')")`)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/mensagens/admin/grupos/predefinidos` | Retorna catálogo dinâmico de RH (Tipos: Membro/Servidor, Órgãos, Setores, Cargos) com contagem de ativos. |
| `GET` | `/api/mensagens/admin/grupos/personalizados` | Lista grupos personalizados ativos. |
| `POST` | `/api/mensagens/admin/grupos/personalizados` | Cria grupo personalizado com nome, descrição e lista de matrículas. |
| `PUT` | `/api/mensagens/admin/grupos/personalizados/{id}` | Atualiza dados e membros do grupo personalizado. |
| `DELETE` | `/api/mensagens/admin/grupos/personalizados/{id}` | Inativa/remove grupo personalizado. |
| `GET` | `/api/mensagens/admin/destinatarios/buscar?q=...` | Busca servidores/membros ativos por nome ou matrícula para `UNICAST` e montagem de grupos. |


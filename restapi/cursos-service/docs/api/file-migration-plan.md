# Plano de Migracao de Endpoints de Arquivo

Data: 2026-07-24.

## Situacao atual

O contrato atual usa `GET /api/arquivos/{id}` e `POST /api/arquivos/upload` em varios fluxos do frontend. A correcao desta etapa manteve esses contratos, mas adicionou autorizacao por recurso no backend.

## Recomendacao

Migrar progressivamente para endpoints contextualizados, sem quebrar o frontend atual:

| Contrato atual | Endpoint recomendado | Motivo |
|---|---|---|
| `GET /api/arquivos/{id}` para programacao | `GET /api/solicitacoes/{solicitacaoId}/programacao` | permite validar solicitacao + dono antes de localizar arquivo |
| `GET /api/arquivos/{id}` para comprovante | `GET /api/cursos/{cursoId}/arquivos-comprobatorios/{arquivoId}` | evita download por ID fora do curso |
| `GET /api/arquivos/{id}` para foto/curriculo de ministrante em solicitacao | `GET /api/solicitacoes/{solicitacaoId}/ministrantes/{ministranteId}/foto` e `/curriculo` | permite validar usuario + solicitacao + ministrante + arquivo |
| `GET /api/arquivos/{id}` para foto/curriculo de ministrante cadastrado | `GET /api/ministrantes/{ministranteId}/foto` e `/curriculo` com `cursos.manter.cursos` | separa cadastro administrativo de anexos da solicitacao |
| `GET /api/public/arquivos/{id}` | `GET /api/public/cursos/{hash}/imagem-cabecalho` | remove ID sequencial da superficie publica |
| `POST /api/arquivos/upload` | uploads por recurso | registra contexto no momento do upload |

## Fases

1. Manter endpoint generico com resolvedor atual e negar por padrao.
2. Criar endpoints contextualizados em paralelo.
3. Alterar frontend para consumir endpoints contextualizados.
4. Marcar o endpoint generico como legado.
5. Remover suporte generico apenas apos confirmar ausencia de consumidores.

## Pontos que exigem confirmacao

- Se imagens privadas de curso em construcao devem ser acessiveis apenas ao CEAF ou tambem a outros perfis internos.
- Como registrar owner/finalidade sem alterar indevidamente o schema compartilhado `ged.arquivo`.

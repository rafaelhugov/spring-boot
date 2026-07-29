# Plano de Testes - Cursos

## Objetivo

Garantir que cursos, solicitacoes, inscricoes, participantes, ministrantes, frequencia, documentos, dashboards, cadastros e fluxos publicos funcionem com seguranca, rastreabilidade e regressao automatizada.

## Pre-requisitos

- Java 21, Maven, Node compativel com `^20.19.0 || >=22.12.0`.
- PostgreSQL de teste via Testcontainers para integracao.
- Variaveis de ambiente isoladas para Auth API, MPVirtual, SMTP e PDF engine.
- Usuarios de teste com permissoes: solicitante `cursos.acesso`, gestor `cursos.manter.cursos`, dashboard `cursos.dashboard`, cadastro `cursos.cadastros`, sem permissao.

## Mapa da aplicacao

| Area | Backend | Frontend | Observacoes |
|---|---|---|---|
| Publico | `PublicController` | `CursosPublicosView`, `InscricaoPublicaView`, `RegistroFrequenciaView` | Sem login; alto risco de abuso/rate limit |
| Autenticacao | Auth externo e Security comum | `authStore`, `authClient`, router guard | Token enviado como `Token` |
| Dashboard | `DashboardController/Service` | `HomeView`, componentes dashboard | Requer `cursos.dashboard` |
| Solicitacoes | `SolicitacaoCursoController/Service` | dashboard/form/dialogs | Protocolo, conversao para curso, PDF |
| Cursos | `CursoController/Service` | list/form/detail | CRUD, status, carga horaria, arquivos |
| Ministrantes | `MinistranteController/Service` | dialogs e autocomplete | Interno RH e externo por CPF |
| Participantes/Inscricoes | `ParticipanteController`, `InscricaoInternoController`, `InscricaoService` | participantes, meus cursos, inscricoes | Interno/externo, situacao, acessibilidade |
| Frequencia | `AulaController`, servicos aula/chamada/frequencia | aulas, QR, registro publico | Tokens, concorrencia e duplicidade |
| Documentos/certificados | `DocumentosController`, `CertificadoController`, services PDF | dialogs de documentos | PDF, download, templates |
| Cadastros/lookup | entidades tutorias, lookup | cadastros e selects | Dados de dominio |
| Arquivos | `ArquivoController/Service`, GED | uploads e downloads | Multipart 5 MB |
| Manuais | `ManualController/Service` | ManualLayout/views | Token sistemico em query para renderizacao |

## Responsabilidades

- QA: desenho de cenarios, exploratorio, regressao e aceite.
- Backend: unitarios, integracao, API e contratos.
- Frontend: componentes, composables, acessibilidade e E2E.
- DevSecOps: pipeline, SAST/DAST, dependency audit, performance e observabilidade.


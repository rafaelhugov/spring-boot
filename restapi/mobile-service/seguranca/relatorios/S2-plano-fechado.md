# Plano fechado da S2 - Eliminar tokens de URLs

Objetivo: migrar todos os endpoints sensiveis para `Authorization: Bearer` sem indisponibilidade, mantendo compatibilidade temporaria com `?token=` ate os clientes serem migrados.

## Contrato alvo S2

| Item | Decisao S2 |
|---|---|
| Header padrao | `Authorization: Bearer <token>` |
| Compatibilidade temporaria | Backend aceita `?token=` somente durante S2-T01 a S2-T09. |
| URLs de foto/PDF | Nao podem conter token de sessao. Usar request autenticado por header no app ou URL temporaria curta se tecnicamente necessario. |
| Logs | Nunca registrar token, Authorization ou URL completa com query sensivel. |
| Erro sem credencial | 401 para token ausente/invalido; manter divergencias antigas apenas enquanto compatibilidade estiver ativa. |
| Bloqueio de regressao | Teste/scan deve falhar se `token` de sessao reaparecer em query/URL. |

## Ordem de execucao

| Tarefa | Escopo | Alteracao minima | Testes exigidos |
|---|---|---|---|
| S2-T01 | Backend comum/mobile | Criar resolvedor de token que aceite Bearer e query legado nos controllers sensiveis. | Bearer valido, ausente, invalido e query legado valido. |
| S2-T02 | Backend/Flutter | Sanitizar logs e URLs geradas; criar testes/scan para `?token=`. | Teste negativo de log/scan. |
| S2-T03 | Perfil | Flutter usa Bearer; backend aceita Bearer. | Perfil carrega; sem token em URL de foto. |
| S2-T04 | Carteira | Dados e foto sem token em query. | Dados/foto autorizados; 401 sem Bearer. |
| S2-T05 | Contracheque/PDF | Dados e PDF sem token em URL. | PDF obtido sem query; 401 sem Bearer. |
| S2-T06 | Designacoes | Migrar repository/controller. | Usuario autenticado ok; sem credencial 401. |
| S2-T07 | Concessoes/Folgas | Migrar endpoints RH. | Operacoes positivas e negativas. |
| S2-T08 | Pagamentos/demais | Migrar pagamentos e achados remanescentes. | Exercicios/pagamentos com Bearer; sem token 401. |
| S2-T09 | Regressao | Scan bloqueia query token. | Busca automatizada passa. |
| S2-T10 | Backend | Desativar query token legado. | Query token rejeitado. |
| S2-T11 | Limpeza | Remover codigo legado. | Flutter test, Maven test e novos testes verdes. |

## Rollback por tarefa

- Manter compatibilidade query ate S2-T10 permite rollback do Flutter para versao anterior.
- Antes de S2-T10, rollback e revert por commit da tarefa.
- Depois de S2-T10, rollback exige reativar compatibilidade temporaria no resolvedor de token.

## Criterios de aceite da S2

- Nenhuma ocorrencia de token de sessao em query string, URL de PDF/foto, link compartilhavel ou log.
- Todos os modulos sensiveis usam `Authorization: Bearer`.
- Backends sensiveis retornam 401 quando credencial ausente/invalida.
- Regressao completa verde.


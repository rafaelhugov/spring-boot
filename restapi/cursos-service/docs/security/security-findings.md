# Achados de Seguranca - cursos-service

Data: 2026-07-24.

## Riscos Corrigidos

| ID | Risco | Evidencia antes | Correcao | Evidencia de teste |
|---|---|---|---|---|
| SF-002 | IDOR em download privado por ID numerico | arquivo vinculado a entidade conhecida podia ser liberado sem propriedade real | resolvedor por categoria + validacao de solicitante/solicitacao/arquivo | `usuarioBNaoDeveBaixarArquivo100DaSolicitacaoDoUsuarioA`, `outroUsuarioNaoBaixaCurriculoDeMinistranteDaSolicitacaoDoUsuarioA` |
| SF-003 | Curriculo/foto de ministrante expostos a usuario interno comum | `cursos.acesso` era amplo demais para o endpoint generico | usuario comum acessa apenas arquivos da propria solicitacao | `solicitanteAcessaCurriculoDeMinistranteDaPropriaSolicitacao`, `outroUsuarioNaoBaixaFotoDeMinistranteDaSolicitacaoDoUsuarioA` |
| SF-004 | Comprovante/template baixavel por fluxo generico | endpoint generico podia contornar regra administrativa | comprovante e template exigem `cursos.manter.cursos` | `usuarioComCursosAcessoNaoBaixaArquivoComprobatorioAdministrativo`, `participanteAutenticadoNaoBaixaTemplateCertificado` |
| SF-005 | Documento administrativo sem protecao explicita | `DocumentosController.gerarDocumento` sem `@PreAuthorize` proprio | `@PreAuthorize("hasAuthority('cursos.manter.cursos')")` | `usuarioComCursosAcessoNaoGeraDocumentoAdministrativo`, `administradorGeraDocumentoAdministrativo` |
| SF-006 | Manual administrativo aceitava token em query | controller recebia query token; service montava URL com token sistêmico | query token removido; service nao monta URL com token; Auth provider nao loga token | `manualAdministrativoComTokenNaQueryRetorna401`, `manualAdministrativoComHeaderTokenAutorizadoRetorna200` |
| SF-010 | Log de autenticacao expunha token parcial | `TokenAuthenticationProvider` registrava prefixo do token | log registra apenas `usuarioId` e quantidade de permissoes | verificacao em XML Surefire sem `DEBUG-TOKEN`/`Token:` |

## Riscos Parcialmente Corrigidos

| Risco | Status atual | Pendencia |
|---|---|---|
| ID numerico em download | ID nao autoriza sozinho | migrar para endpoints contextualizados/opacos |
| Upload generico | valida arquivo e bloqueia download orfao | nao registra owner/finalidade; criar upload contextualizado |
| Arquivo ambiguo | nega por padrao | criar rotina operacional para corrigir dados ambiguos |
| Manual administrativo | backend exige header e remove query token | frontend `ManualTopBar.vue` ajustado para chamada autenticada por `apiClient` e resposta `blob` |

## Riscos Pendentes

| ID | Risco | Evidencia | Recomendacao |
|---|---|---|---|
| SF-007 | Endpoints publicos sem rate limiting | `/api/public/**` segue publico | aplicar rate limit para inscricao, frequencia, consulta por CPF e confirmacao |
| SF-008 | CSRF globalmente desabilitado | `SecurityConfig` usa stateless token | manter stateless conscientemente ou revisar se houver cookie |
| SF-011 | `ged.arquivo` sem owner/finalidade | entidade `Arquivo` nao possui contexto | avaliar tabela auxiliar ou endpoints contextualizados |
| SF-012 | Teste de token expirado real depende do Auth API | repo simula token invalido via mock | validar contrato em homologacao sem registrar token/PII |

## Decisoes Confirmadas

- `cursos.acesso` nao concede download geral.
- `cursos.manter.cursos` e a authority administrativa para arquivos CEAF.
- `cursos.analisar.solicitacoes` nao foi usado como bypass administrativo.
- Foto/curriculo de ministrante nao sao publicos.
- Publicacao do curso torna publica apenas a imagem de cabecalho, nao template ou comprovante.

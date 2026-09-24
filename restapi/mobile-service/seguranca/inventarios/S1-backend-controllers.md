# S1-T03/S1-T04 - Mapa backend de controllers, rotas e autorizacao

## Spring Security atual

Arquivo: `restapi-common/src/main/java/mppb/restapi/common/infrastructure/config/SecurityConfig.java`.

Comportamento:
- Se profile contem `insecure`, toda rota e `permitAll`.
- Em `prod`, exige autenticacao fora de `PUBLIC_URLS`.
- `PUBLIC_URLS` inclui rotas sensiveis: carteiras, contracheques, recursos humanos, designacoes, notificacoes/dispositivos e pagamentos.
- Filtro atual reconhece `Authorization: Token`, nao `Bearer`, no `CustomBasicAuthenticationFilter`.

## Rotas publicas reais

| Rota | Controller | Classificacao | Observacao |
|---|---|---|---|
| `/api/public/health` | `PublicHealthController` | Publico | Health basico. |
| `/api/public/noticias` | `NoticiaController` | Publico | Noticias. |
| `/api/public/diarios-oficiais/recentes` | `PublicoDiarioOficialController` | Publico | Diario oficial. |
| `/api/public/v1/captcha` | `CaptchaController` | Publico | Captcha em memoria, uso unico. |
| `/api/public/v1/processos/filtros` | `ProcessoConsultaPublicaController` | Publico | Filtros. |
| `/api/public/v1/processos/consulta` | `ProcessoConsultaPublicaController` | Publico sensivel a abuso | Rate limit simples por IP em memoria. |
| `/api/public/v1/processos/{id}/detalhes` | `ProcessoDetalhesController` | Publico com captcha | Requer captcha token/valor. |
| `/api/public/certidoes-extrajudiciais/**` | `CertidaoExtrajudicialController` | Publico | Gera/consulta certidoes. |
| `/carteiras-funcionais/public/logo` | `CarteiraFuncionalController` | Publico | Imagem institucional. |
| `/carteiras-funcionais/public/foto/{md5}` | `CarteiraFuncionalController` | Publico sensivel | Foto por hash; requer avaliacao BOLA/privacidade. |

## Rotas sensiveis com autenticacao manual

| Rota | Controller | Dado/acao | Validacao atual | Status sem/invalid token |
|---|---|---|---|---|
| `/api/perfil/dados` | `PerfilUsuarioController` | Perfil autenticado | `@RequestParam token` -> `auth-api` | 401 |
| `/carteiras-funcionais/dados` e `/api/carteiras-funcionais/dados` | `CarteiraFuncionalController` | Carteira funcional | `@RequestParam token` -> `auth-api` | 401 |
| `/carteiras-funcionais/foto` e `/api/carteiras-funcionais/foto` | `CarteiraFuncionalController` | Foto autenticada | `@RequestParam token` -> `auth-api` | 401 |
| `/api/designacoes/ativas` | `DesignacaoController` | Designacoes ativas | `@RequestParam token` -> matricula | 401 |
| `/contracheques/**` e `/api/contracheques/**` | `ContrachequeController` | Contracheque/PDF | `@RequestParam token` -> login -> matricula | 403/400 |
| `/pagamentos/**` e `/api/pagamentos/**` | `PagamentosController` | Pagamentos por CPF | `@RequestParam token` -> login -> CPF | 401/400 |
| `/api/recursos-humanos/**` | `ConcessaoFolgaController` | Concessoes/ausencias/operacoes | `@RequestParam token` -> login -> matricula | 403 |
| `/api/notificacoes/dispositivos/**` | `DispositivoNotificacaoController` | Registro/diagnostico/desativacao FCM | Header `Authorization: Bearer` manual + SecurityContext quando existe | 401/403 conforme caso |

## Integracoes externas

| Integracao | Uso | Trust boundary |
|---|---|---|
| `auth-api` | `autoriza` e `consulta-autorizacao` | Autoridade de autenticacao atual. |
| MPVirtual/RH | Concessoes e operacoes | Recebe token original ou login conforme `mpvirtual.recursos-humanos.auth-mode`. |
| JCR | Fotos/documentos carteira | Usa credenciais tecnicas de backend. |
| Firebase Admin | FCM | Envio de notificacoes. |
| Google Wallet | Carteira funcional | Credentials path local/configurado. |

## Lacunas para S3

- Reduzir `PUBLIC_URLS` para publicos reais.
- Implementar filtro/provider Bearer centralizado.
- Popular `SecurityContext` com login/matricula/perfis necessarios.
- Definir 401 vs 403 padrao antes da migracao em lote.


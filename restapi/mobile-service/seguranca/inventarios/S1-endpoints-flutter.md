# S1-T01 - Inventario de endpoints chamados pelo Flutter

Base URL principal: `String.fromEnvironment('API_URL', defaultValue: 'https://aplicativo-api.mppb.mp.br')` em `lib/core/network/api_client.dart`.

## Autenticacao

| Modulo | Metodo | URL | Transporte atual |
|---|---:|---|---|
| Login | POST | `https://auth-api.mppb.mp.br/acesso/autoriza` | Usuario/senha no corpo JSON. |

## Endpoints sensiveis autenticados

| Modulo | Metodo | URL | Arquivo Flutter | Credencial atual |
|---|---:|---|---|---|
| Perfil | GET | `/api/perfil/dados` | `lib/features/perfil/repositories/perfil_repository.dart` | Query `token` + header `Authorization: Token`. |
| Perfil foto | GET | `/carteiras-funcionais/foto?token=...` | `lib/features/perfil/repositories/perfil_repository.dart` | Token em URL gerada. |
| Carteira dados | GET | `/carteiras-funcionais/dados` | `lib/features/carteira/providers/carteira_provider.dart` | Query `token`; interceptor tambem adiciona Bearer. |
| Carteira foto | GET | `/carteiras-funcionais/foto?token=...` | `lib/features/carteira/providers/carteira_provider.dart` | Token em URL gerada. |
| Designacoes | GET | `/api/designacoes/ativas` | `lib/features/designacoes/repositories/designacoes_repository.dart` | Query `token` + header `Authorization: Token`. |
| Contracheque competencias | GET | `/contracheques/competencias` | `lib/features/contra_cheque/providers/contra_cheque_provider.dart` | Query `token`; interceptor tambem adiciona Bearer. |
| Contracheque dados | GET | `/contracheques/{mes}/{ano}` | `lib/features/contra_cheque/providers/contra_cheque_provider.dart` | Query `token`; interceptor tambem adiciona Bearer. |
| Contracheque PDF | GET | `/contracheques/{mes}/{ano}/pdf?token=...` | `lib/features/contra_cheque/providers/contra_cheque_provider.dart` | Token em URL gerada. |
| Pagamentos exercicios | GET | `/pagamentos/exercicios` | `lib/features/pagamentos/providers/pagamentos_provider.dart` | Query `token` quando existe; interceptor tambem adiciona Bearer. |
| Pagamentos ano | GET | `/pagamentos/{exercicio}` | `lib/features/pagamentos/providers/pagamentos_provider.dart` | Query `token` quando existe; interceptor tambem adiciona Bearer. |
| RH concessoes | GET | `/api/recursos-humanos/minhas-concessoes-folga` | `lib/features/concessao_afastamento/repositories/concessao_afastamento_repository.dart` | Query `token` + header `Authorization: Token`. |
| RH ausencias | GET | `/api/recursos-humanos/minhas-ausencias` | `lib/features/concessao_afastamento/repositories/concessao_afastamento_repository.dart` | Query `token` + header `Authorization: Token`. |
| RH sugestoes | GET | `/api/recursos-humanos/concessoes/{id}/sugestoes-datas-finais` | `lib/features/concessao_afastamento/repositories/concessao_afastamento_repository.dart` | Query `token` + header `Authorization: Token`. |
| RH solicitar gozo | POST | `/api/recursos-humanos/concessoes/{id}/solicitacoes` | `lib/features/concessao_afastamento/repositories/concessao_afastamento_repository.dart` | Query `token` + header `Authorization: Token`. |
| RH operacoes | GET | `/api/recursos-humanos/solicitacoes/{id}/operacoes` | `lib/features/concessao_afastamento/repositories/concessao_afastamento_repository.dart` | Query `token` + header `Authorization: Token`. |
| RH executar operacao | POST | `/api/recursos-humanos/solicitacoes/{id}/{operacao}` | `lib/features/concessao_afastamento/repositories/concessao_afastamento_repository.dart` | Query `token` + header `Authorization: Token`. |
| Registro FCM | POST | `/api/notificacoes/dispositivos` | `lib/core/notifications/device_registration_repository.dart` | Header `Authorization: Bearer` via interceptor. |
| Diagnostico FCM | POST | `/api/notificacoes/dispositivos/diagnostico` | `lib/core/notifications/device_registration_repository.dart` | Header `Authorization: Bearer` via interceptor. |
| Desativar FCM | DELETE | `/api/notificacoes/dispositivos/{identificador}` | `lib/core/notifications/device_registration_repository.dart` | Header `Authorization: Bearer` via interceptor. |

## Endpoints publicos

| Modulo | Metodo | URL | Observacao |
|---|---:|---|---|
| Noticias | GET | `/api/public/noticias` | Publico. |
| Diario oficial | GET | `/api/public/diarios-oficiais/recentes` | Publico. |
| Processo filtros | GET | `/api/public/v1/processos/filtros` | Publico. |
| Processo consulta | POST | `/api/public/v1/processos/consulta` | Publico com captcha/rate limit. |
| Captcha processo | GET | `/api/public/v1/captcha` | Publico. |
| Processo detalhe | GET | `/api/public/v1/processos/{id}/detalhes` | Usa captchaToken/captchaValue em query; nao e token de sessao. |
| Certidao extrajudicial | POST/GET | `/api/public/certidoes-extrajudiciais/**` | Publico. |
| Ouvidoria | GET/POST | `https://ouvidoria-api.mppb.mp.br/**` | API externa ao mobile-service. |
| GED arquivo diario | GET | `https://ged-api.mppb.mp.br/public/arquivos/{id}/file` | Publico externo. |

## Observacoes

- O `dioProvider` adiciona `Authorization: Bearer <auth_token>` quando nao existe header `Authorization`.
- Modulos que passam `Authorization: Token` impedem o interceptor de usar Bearer.
- URLs de foto/PDF com token sao prioridade da S2 porque o segredo entra na URL final, fora do controle do interceptor.


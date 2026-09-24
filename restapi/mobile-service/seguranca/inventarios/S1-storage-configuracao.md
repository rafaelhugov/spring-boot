# S1-T06 - Inventario de storage Flutter e configuracao

## Flutter Secure Storage

| Chave | Arquivo | Finalidade | Risco |
|---|---|---|---|
| `auth_token` | `features/auth/providers/auth_provider.dart`, `core/network/api_client.dart` | Sessao/API. | Deve ter expiracao/renovacao/revogacao definida; hoje tambem vai em URL. |
| `auth_username` | `features/auth/providers/auth_provider.dart` | Identificar usuario atual/cache. | Baixo/medio; PII identificadora. |
| `saved_username` | `features/auth/providers/auth_provider.dart` | Conta vinculada a biometria legada. | Baixo/medio. |
| `saved_password` | `features/auth/providers/auth_provider.dart` | Login biometrico legado. | Critico; senha persistida. |
| `biometrics_enabled` | `features/auth/providers/auth_provider.dart` | Flag local de biometria. | Nao autoriza sozinha, mas pode confundir fluxo. |
| `biometrics_asked` | `features/auth/providers/auth_provider.dart` | Controle de prompt. | Baixo. |
| `carteira_cache_data` | `features/carteira/providers/carteira_provider.dart` | Cache offline da carteira. | Pode conter dados pessoais/sensiveis. |
| `carteira_cache_username` | `features/carteira/providers/carteira_provider.dart` | Dono do cache. | Evita mistura basica de usuario. |
| `perfil_cache_data` | `features/perfil/providers/perfil_provider.dart` | Cache offline do perfil. | Pode conter PII. |
| `perfil_cache_username` | `features/perfil/providers/perfil_provider.dart` | Dono do cache. | Evita mistura basica de usuario. |
| `push_installation_id` | `core/notifications/device_registration_provider.dart` | Identificador local da instalacao para FCM. | Persistente; deve ser revogavel/desativavel. |
| `notification_read_keys` | `core/notifications/notification_center_provider.dart` | Itens lidos. | Baixo; pode revelar interesse/noticias. |

## Configuracao Android

| Item | Estado atual | Fase |
|---|---|---|
| `usesCleartextTraffic` | `true` global no manifest principal. | S4 |
| Permissoes | `INTERNET`, `USE_BIOMETRIC`, `POST_NOTIFICATIONS`. | S8 |
| Activity principal | `exported=true` por launcher. | S8 |
| Flavors | `producao` e `homolog`. | S4/S7 |
| Assinatura | `key.properties` local; keystore local nao rastreado. | S6/S7 |

## Configuracao backend

| Item | Estado atual | Risco | Fase |
|---|---|---|---|
| Profile default | `${SPRING_PROFILES_ACTIVE:dev}` | Startup inseguro por omissao. | S4 |
| Profile `insecure` | Desabilita exigencia de token. | Uso acidental bloqueia release. | S4 |
| `JPA_SHOW_SQL` | Default `true`. | SQL/dados em log. | S4 |
| Datasource default | localhost/postgres/senha. | Default fraco fora de dev. | S4 |
| JCR default | `admin/admin`. | Credencial default. | S4/S6 |
| CORS | `allow-origin:*`, credentials false. | Precisa revisar por ambiente. | S4/S8 |
| Google credentials path | `google-credentials.json`. | Arquivo local sensivel. | S6 |


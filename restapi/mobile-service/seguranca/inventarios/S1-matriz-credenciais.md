# S1-T02 - Matriz de transporte de credenciais

| Credencial | Origem | Transporte atual | Risco | Fase de tratamento |
|---|---|---|---|---|
| Usuario/senha | Login Flutter | Corpo JSON para `auth-api` | Necessario no login primario; erro deve ser generico e logs sanitizados. | S5/S8 |
| `auth_token` | Resposta do `auth-api` | Secure Storage + header Bearer automatico + query legado | Vazamento por URL/log/proxy quando usado em query. | S2 |
| `auth_token` em header `Token` | Repositorios especificos | `Authorization: Token <token>` | Contrato paralelo ao Bearer; dificulta centralizacao. | S2/S3 |
| `auth_token` em URL | Foto carteira/perfil e PDF contracheque | Query string montada manualmente | Alto risco de exposicao em historico, logs e apps externos. | S2 |
| Token FCM | Firebase Messaging | Corpo JSON para registrar dispositivo | Sensivel para push; testes ja verificam nao logar valor bruto no app. | S6/S8 |
| Identificador de instalacao push | Flutter Secure Storage | Corpo JSON/rota DELETE | Identificador persistente; nao deve autorizar sozinho. | S3/S5 |
| `saved_password` | Flutter Secure Storage | Uso local para login biometrico | Critico: senha persistida reversivel. | S5 |
| Captcha de processo | Backend captcha | Query `captchaToken`/`captchaValue` para detalhe publico | Nao e token de sessao, mas pode aparecer em rota interna do app. | S8 |
| Service account Google Wallet/Firebase Admin | Backend local/env | `google-credentials.json`/credentials path | Segredo tecnico; nao rastreado, mas deve ser gerido fora do repo. | S6 |
| Keystore Android | Flutter local | `android/key.properties` + `.keystore` local | Segredo de assinatura; nao rastreado, deve ficar em cofre/CI. | S6/S7 |

## Controles ja existentes

- `ConsultaAutorizacaoRequestDto.toString()` mascara o token.
- Registro FCM usa hash prefix/tamanho nos logs do backend, nao token bruto.
- `DeviceRegistrationManager` possui teste que garante que token FCM nao aparece em logs Flutter.

## Lacunas

- Nao ha teste automatizado falhando quando `?token=` reaparece.
- Nao ha contrato unico para Bearer em todos os modulos.
- Nao ha confirmacao local de refresh/revogacao do token de sessao.


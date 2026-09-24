# S1 - Inventario, ameaca e contrato de autenticacao

Data: 2026-09-24
Branch: `seguranca/s1-inventario-ameaca-contrato`
Escopo:
- Flutter: `/home/rafaelhugo/projetosMPPB/app_mppb_flutter`
- Backend: `/home/rafaelhugo/projetosMPPB/spring-boot/restapi/mobile-service`
- Comum: `/home/rafaelhugo/projetosMPPB/spring-boot/restapi/restapi-common`

## Resultado por tarefa

| Tarefa | Status | Saida |
|---|---|---|
| S1-T01 | Concluido | Inventario de chamadas Flutter em `seguranca/inventarios/S1-endpoints-flutter.md`. |
| S1-T02 | Concluido | Matriz de transporte de credenciais em `seguranca/inventarios/S1-matriz-credenciais.md`. |
| S1-T03 | Concluido | Mapa de controllers, rotas e autorizacao em `seguranca/inventarios/S1-backend-controllers.md`. |
| S1-T04 | Concluido | Classificacao publico/autenticado/sensivel no inventario backend. |
| S1-T05 | Pendente externo | Contrato real do `auth-api` no codigo local cobre `autoriza` e `consulta-autorizacao`; expiracao, refresh e revogacao nao foram encontrados no cliente local e precisam de confirmacao institucional. |
| S1-T06 | Concluido | Inventario de storage Flutter em `seguranca/inventarios/S1-storage-configuracao.md`. |
| S1-T07 | Concluido | Threat model inicial em `seguranca/modelos-ameaca/S1-threat-model-inicial.md`. |
| S1-T08 | Parcial | Contrato alvo preliminar definido em `seguranca/relatorios/S2-plano-fechado.md`; depende de confirmacao do `auth-api` para biometria/refresh na S5. |
| S1-T09 | Concluido | Baseline registrado em `seguranca/evidencias/S1/baseline-testes.md`. |

## Achados confirmados

| ID | Achado | Evidencia local | Severidade |
|---|---|---|---|
| A-01 | Tokens de autenticacao em query string. | Perfil, carteira, designacoes, contracheque, pagamentos, recursos humanos e URLs de foto/PDF usam `token` em query. | Alta |
| A-02 | Rotas sensiveis em `PUBLIC_URLS` e autenticacao manual em controllers. | `SecurityConfig` permite carteiras, contracheques, recursos humanos, designacoes, pagamentos e notificacoes/dispositivos; controllers validam token manualmente. | Alta |
| A-03 | Android permite cleartext global. | `android:usesCleartextTraffic="true"` no manifest principal. | Alta |
| A-04 | Senha persistida para biometria. | Chave `saved_password` gravada em `AuthNotifier`. | Critica |
| A-05 | Defaults inseguros de producao/configuracao. | Perfil default `dev`, `JPA_SHOW_SQL:true`, `JCR_USER/JCR_PASSWORD:admin/admin`. | Alta |
| A-06 | Segredos locais no ambiente de desenvolvimento. | `key.properties`, keystore, `.env.dev` e `google-credentials.json` existem localmente; nao rastreados. | Media/Alta |
| A-07 | Pipeline de seguranca nao localizado nos projetos. | Nao foram encontrados `.gitlab-ci.yml`, `.github/workflows` ou `Jenkinsfile` nos dois escopos. | Alta |

## Divergencias e lacunas

- O plano mestre pede arquitetura alvo de access token, refresh, revogacao e biometria. O codigo local do `restapi-common` nao mostra endpoints de refresh/revogacao no `AuthServiceClient`; isso deve ser confirmado antes da S5.
- O app Flutter tem `ApiClient` que adiciona `Authorization: Bearer`, mas varios modulos sensiveis ainda enviam `Authorization: Token` e query `token`. A S2 deve manter compatibilidade transitoria.
- A autenticacao centralizada ja existe parcialmente para `Authorization: Token` no `restapi-common`, mas as rotas sensiveis atuais estao em `PUBLIC_URLS`, portanto o filtro nao e o ponto de enforcement desses endpoints.
- Os SQLs locais de 2026-09-24 foram ignorados no `.gitignore` por pedido operacional; nao fazem parte da S1.

## Baseline

- Flutter: `flutter test` passou com 110 testes.
- Backend: `mvn -q test` passou.
- Contagem de arquivos de teste: 18 no Flutter e 17 no backend.

## Criterio de saida S1

Status: parcialmente atendido.

Atendido:
- Fluxos e endpoints sensiveis foram mapeados.
- Baseline funcional esta verde.
- Riscos iniciais estao classificados.
- Plano fechado da S2 foi preparado.

Pendente para aceite formal da S1:
- Confirmar com responsavel do `auth-api` se ha expiracao, refresh, revogacao e contrato de sessao nao representado no codigo local.
- Aprovar que a S2 iniciara somente pela eliminacao de token em URL, sem alterar ainda modelo de sessao/biometria.


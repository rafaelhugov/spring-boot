# S8 - Matriz unica de rastreabilidade S1-S8

Data: 2026-09-24

| ID | Requisito/risco | Controle implementado | Teste/evidencia | Resultado | Risco residual |
|---|---|---|---|---|---|
| RT-01 | Inventariar endpoints e credenciais | S1 inventarios backend/Flutter/storage | `inventarios/S1-*.md` | Coberto | Manter atualizado a cada endpoint novo |
| RT-02 | Token nao pode trafegar em URL | S2 migra para Bearer/header e gate S7 | `evidencias/S2/remocao-token-url.md`, `security_regression.sh` | Coberto local | Validar logs/proxy/APM reais |
| RT-03 | Endpoints sensiveis exigem autorizacao central | S3 Spring Security | `evidencias/S3/spring-security-centralizado.md` | Coberto local | BOLA/BFLA multiusuario pendente |
| RT-04 | Producao sem defaults inseguros | S4/S6 fail-fast e segredos externos | `hardening-producao-backend.md`, `gestao-segredos-backend.md` | Coberto local | Validar configuracao efetiva prod-like |
| RT-05 | Biometria nao pode recuperar senha | S5 remove fluxo de senha salva | Evidencia Flutter S5, teste `biometric_password_storage_test.dart` | Coberto | Novo contrato biometrico seguro pendente |
| RT-06 | Segredos fora do repositorio | S6 `.gitignore`, config e secret scan | Evidencias S6/S7/S8 | Coberto no working tree | Historico Git/rotacao real pendente |
| RT-07 | Pipeline e gates | S7 GitLab CI versionado e scripts locais | Evidencias S7 | Implementado | Runner GitLab ausente |
| RT-08 | MASVS mobile | Matriz S8 mobile | `S8-mobile-masvs-mastg.md` | Parcial | iOS/aparelho/DAST mobile pendentes |
| RT-09 | ASVS/API Top 10 backend | Matriz S8 backend | `S8-backend-asvs-api-top10.md` | Parcial | DAST e casos multiusuario pendentes |
| RT-10 | Threat model por fluxo | S8 threat models | `S8-threat-model-fluxos-sensiveis.md` e threat model mobile | Coberto documental | Revisar quando fluxos mudarem |
| RT-11 | Logging/privacidade | Sanitizacao FCM, token sem URL, matriz privacy | Testes FCM e relatorios S8 | Parcial | Auditoria/APM/retencao operacional pendentes |
| RT-12 | Supply chain/release | Secret scan, dependency inventory, build homolog | S7/S8 | Parcial | SBOM CycloneDX, SCA, assinatura/provenance pendentes |
| RT-13 | DAST/pentest | Plano e bloqueio formal | `S8-bloqueios-riscos-residuais.md` | Bloqueado | Homologacao autorizada exigida |
| RT-14 | Regressao final | Testes locais backend/mobile | Evidencias S8 regressao | Coberto local | CI automatico pendente |
| RT-15 | Decisao GO/NO-GO | Relatorio final | `S8-relatorio-final-prontidao.md` | GO condicionado | NO-GO para declaracao final irrestrita sem desbloqueios |

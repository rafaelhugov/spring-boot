# S8 - Bloqueios e riscos residuais

Data: 2026-09-24

Escopo: plano de seguranca do app MPPB+ e `mobile-service`.

## Bloqueios externos

| ID | Item | Status | Dependencia | Impacto |
|---|---|---|---|---|
| BLQ-01 | CI/CD automatico GitLab | Bloqueado | Runner ativo no projeto/grupo | Pipelines S7 existem, mas gates rodam localmente ate infra habilitar runner. |
| BLQ-02 | DAST autenticado | Bloqueado | Ambiente homologacao autorizado, massa de dados e credenciais de teste | Nao e permitido executar active scan contra producao. |
| BLQ-03 | Teste iOS/IPA | Bloqueado | Ambiente Apple, certificado/perfil e aparelho/simulador controlado | Matriz iOS permanece parcial. |
| BLQ-04 | Testes mobile dinamicos avancados | Bloqueado | Aparelho/emulador com laboratorio autorizado | Root/hooking/tampering, backup/restore e filesystem nao foram executados. |
| BLQ-05 | Rotacao real de segredos | Bloqueado | Cofre/processo institucional e owners dos sistemas externos | S6/S7 protegem repo, mas rotacao operacional depende dos donos. |
| BLQ-06 | SBOM/SCA formal CycloneDX | Parcial | Ferramenta institucional ou runner com internet/artefatos | Inventarios de dependencia existem; CycloneDX/SCA formal ainda pendente. |

## Riscos residuais por gate NO-GO

| Gate | Estado | Evidencia | Risco residual |
|---|---|---|---|
| NO-GO-01 - Senha persistida/recuperavel para biometria | Sem evidencia de regressao | Teste Flutter e gate S7/S8 | Biometria segura futura ainda precisa contrato proprio. |
| NO-GO-02 - Token em URL/log | Sem evidencia de regressao local | S2, gates `security_regression.sh` | Validar logs/proxy/APM reais em homologacao. |
| NO-GO-03 - Bypass/BOLA/BFLA/BOPLA | Parcial | Spring Security, testes unitarios/controllers | Falta suite multiusuario em homologacao. |
| NO-GO-04 - Cleartext em release | Sem evidencia Android local | `android_cleartext_manifest_test.dart` | iOS ATS e captura MITM ainda pendentes. |
| NO-GO-05 - Segredo real exposto | Sem evidencia em escopo mobile/common | Secret scan S7/S8 | Historico Git e rotacao real exigem ferramenta/processo. |
| NO-GO-06 - CVE High/Critical exploravel | Parcial | Dependencias inventariadas; SCA formal pendente | Classificacao exploravel depende de SCA. |
| NO-GO-07 - Build prod debug/dev/defaults inseguros | Parcial | S4/S6, package Maven, build homolog | Release final assinado/prod-like pendente. |
| NO-GO-08 - DAST/pentest critico | Bloqueado | DAST nao executado por falta de homologacao autorizada | Nao afirmar go final sem DAST ou aceite formal. |
| NO-GO-09 - Matriz critica sem evidencia | Parcial | Matrizes S8 mobile/backend | Itens dinamicos marcados parcial/bloqueado. |
| NO-GO-10 - SBOM ausente | Parcial | `dart pub deps --json` e Maven dependency tree previstos no S7 | SBOM CycloneDX final por release pendente. |

## Decisao tecnica atual

Estado: **GO condicionado para continuar o plano**, nao go final de producao.

Motivo: controles principais implementados localmente e sem regressao nos testes executados, mas a prontidao de producao ainda depende de homologacao autorizada, CI/CD funcional, SCA/SBOM formal e testes dinamicos mobile.

## Proximas acoes recomendadas

1. Habilitar runner GitLab Docker ou adaptar pipeline para runner shell institucional.
2. Preparar ambiente de homologacao com usuarios de teste para BOLA/BFLA/BOPLA.
3. Executar DAST autenticado com escopo aprovado e sem dados reais.
4. Gerar SBOM CycloneDX final para APK/JAR e rodar SCA com triagem.
5. Executar testes Android/iOS em aparelho controlado para storage, backup, screenshots, deep links e filesystem.
6. Formalizar aceite temporario dos riscos bloqueados por infraestrutura, com owner e prazo.

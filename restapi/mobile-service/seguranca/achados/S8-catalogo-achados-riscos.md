# S8 - Catalogo de achados e riscos

Data: 2026-09-24

## Achados corrigidos/mitigados

| ID | Achado | Severidade inicial | Tratamento | Evidencia |
|---|---|---|---|---|
| ACH-01 | Token de sessao em query string/URL de foto/PDF | Alta | Migrado para Bearer/header e bloqueado por gate | S2, S7, S8 |
| ACH-02 | Senha salva para login biometrico legado | Critica | Fluxo inseguro removido/desativado; senha nao volta a ser gravada | S5, teste Flutter |
| ACH-03 | Endpoints sensiveis em lista publica | Alta | Spring Security centralizado e rotas sensiveis privadas | S3 |
| ACH-04 | Defaults inseguros de producao | Alta | Hardening e fail-fast | S4/S6 |
| ACH-05 | Segredos/credenciais tecnicas sem governanca clara | Alta | `.gitignore`, externalizacao e secret scan | S6/S7 |
| ACH-06 | Ausencia de pipeline/gates versionados | Media/Alta | `.gitlab-ci.yml` e scripts de gates | S7 |

## Riscos residuais abertos

| ID | Risco | Severidade | Status | Owner sugerido | Dependencia | Proxima acao | Condicao de revisao/fechamento | Evidencia necessaria |
|---|---|---|---|---|---|---|---|---|
| RSK-01 | DAST autenticado nao executado | Alta | Bloqueado | Segurança/QA | Homologacao autorizada, usuarios e massa de teste | Preparar OWASP ZAP/API scan e testes manuais quando necessario | Antes da declaracao final irrestrita | Relatorio DAST triado e reteste de achados |
| RSK-02 | BOLA/BFLA/BOPLA multiusuario nao testado dinamicamente | Alta | Aberto | Backend/QA | Usuarios A/B e dados de homologacao | Criar suite negativa por recurso e papel/claim | Antes da promocao final | Evidencia 401/403/404 para recurso de outro usuario |
| RSK-03 | SCA/SBOM CycloneDX formal ausente | Media/Alta | Aberto | DevOps/Segurança | Runner/ferramenta SCA institucional | Gerar SBOM Flutter/Maven/APK/JAR e triar CVEs | Proximo ciclo de release | SBOM versionado e relatorio SCA |
| RSK-04 | CI/CD automatico sem runner | Media operacional | Bloqueado | Infra/DevOps | Runner GitLab Docker ou pipeline shell | Registrar runner ou adaptar pipeline | Antes de exigir gate automatico | Pipeline executado no GitLab |
| RSK-05 | iOS nao validado | Media | Bloqueado | Mobile/Infra Apple | Certificados, provisioning e ambiente Apple | Executar suite iOS/IPA | Antes de release iOS | Evidencia ATS/Keychain/entitlements/backup |
| RSK-06 | Testes dinamicos Android de storage/backup/screenshots pendentes | Media | Aberto | Mobile/Segurança | Aparelho/emulador controlado | Testar filesystem, backup/restore, screenshots, deep links, intents e MITM | Antes da declaracao mobile final | Relatorio MASTG/dinamico |
| RSK-07 | Validacao de logs/APM/proxy em ambiente real pendente | Media | Aberto | Operacao/DevOps | Ambiente integrado com observabilidade real | Revisar captura de headers/query/body/PII | Antes de producao | Evidencia de sanitizacao e politica de retencao |
| RSK-08 | Rotacao real de segredos depende de processo institucional | Media | Aberto | Owners das integracoes | Cofre/secret manager e owners | Definir SLA, cofre, rotacao e procedimento de incidente | Proximo ciclo operacional | Registro de rotacao/owner/runbook |
| RSK-09 | SAST dedicado ausente | Media/Alta | Aberto | Segurança/DevOps | Ferramenta institucional | Integrar SAST Flutter/Dart/Java/config ao pipeline | Antes de gate final de release | Relatorio SAST com baseline e triagem |
| RSK-10 | Rate limiting distribuido nao comprovado | Media | Aberto | Infra/Backend | Informacao da topologia real do `mobile-service` | Confirmar instancia unica ou implementar controle compartilhado | Antes de DAST/carga final | Evidencia de topologia e teste 429/abuso |
| RSK-11 | Biometria segura futura nao implementada | Media | Aberto | Mobile/Backend/Auth | Contrato de credencial de dispositivo revogavel | Desenhar e implementar antes de reabilitar login biometrico | Antes de oferecer login biometrico | ADR, API, testes multiusuario/revogacao |

## Criterio de encerramento

Um risco residual so deve ser encerrado quando houver evidencia reproducivel, owner, data, decisao e artefato associado. Itens bloqueados por infraestrutura devem receber aceite temporario com prazo de revisao.

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

| ID | Risco | Severidade | Status | Owner sugerido | Proxima acao |
|---|---|---|---|---|---|
| RSK-01 | DAST autenticado nao executado | Alta | Bloqueado | Segurança/QA | Preparar homologacao autorizada |
| RSK-02 | BOLA/BFLA/BOPLA multiusuario nao testado dinamicamente | Alta | Aberto | Backend/QA | Criar usuarios de teste e suite negativa |
| RSK-03 | SCA/SBOM CycloneDX formal ausente | Media/Alta | Aberto | DevOps/Segurança | Integrar ferramenta ou job com runner |
| RSK-04 | CI/CD automatico sem runner | Media operacional | Bloqueado | Infra/DevOps | Registrar runner ou adaptar pipeline |
| RSK-05 | iOS nao validado | Media | Bloqueado | Mobile/Infra Apple | Executar suite iOS/IPA |
| RSK-06 | Testes dinamicos Android de storage/backup/screenshots pendentes | Media | Aberto | Mobile/Segurança | Laboratorio mobile controlado |
| RSK-07 | Validacao de logs/APM/proxy em ambiente real pendente | Media | Aberto | Operacao/DevOps | Revisar captura de headers/query/body |
| RSK-08 | Rotacao real de segredos depende de processo institucional | Media | Aberto | Owners das integracoes | Definir SLA e cofre/rotacao |

## Criterio de encerramento

Um risco residual so deve ser encerrado quando houver evidencia reproducivel, owner, data, decisao e artefato associado. Itens bloqueados por infraestrutura devem receber aceite temporario com prazo de revisao.

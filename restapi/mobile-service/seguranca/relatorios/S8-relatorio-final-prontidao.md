# S8 - Relatorio final de prontidao de seguranca

Data: 2026-09-24

Escopo: App MPPB+ Flutter e backend `mobile-service`.

## Decisao tecnica

**Ciclo local S1-S8: ENCERRADO.**

**GO condicionado para continuidade/homologacao.**

**NO-GO para declaracao irrestrita de producao ate fechamento dos gates externos.**

A avaliacao local S1-S8 nao encontrou regressao critica nos controles implementados. Os principais riscos inicialmente identificados foram tratados ou receberam gates/evidencias: token em URL, senha persistida para biometria, endpoints sensiveis fora do Spring Security, defaults inseguros, segredos em repositorio e ausencia de trilha de evidencias.

A declaracao final irrestrita de producao ainda depende de itens externos ao codigo: runner GitLab ativo, DAST autenticado em homologacao, SCA/SBOM formal por release, testes dinamicos mobile em aparelho controlado e validacao iOS.

## Interpretacao correta da biometria

O ciclo S5 **nao implementou o login biometrico seguro final desejado**.

O que foi concluido:

- fluxo biometrico inseguro baseado em senha persistida removido/desativado;
- impedimento de nova persistencia recuperavel da senha institucional;
- teste e gate contra regressao de `saved_password`.

O que permanece como **evolucao funcional futura**:

- credencial de dispositivo propria, separada da senha e revogavel;
- validacao no backend/auth-api;
- criacao de nova sessao apos autenticacao biometrica local;
- revogacao/substituicao do vinculo quando outro usuario entra com usuario e senha.

Enquanto esse contrato seguro nao existir, a senha institucional nao pode ser armazenada nem recuperada para login biometrico.

## Resultado por fase

| Fase | Resultado | Evidencia principal |
|---|---|---|
| S1 - Inventario, ameaca e contrato | Concluida | `inventarios/*`, `modelos-ameaca/S1-threat-model-inicial.md`, `relatorios/S1-relatorio-inventario-ameaca-contrato.md` |
| S2 - Eliminar tokens de URLs | Concluida | `evidencias/S2/remocao-token-url.md` |
| S3 - Spring Security centralizado | Concluida | `evidencias/S3/spring-security-centralizado.md` |
| S4 - Hardening de producao | Concluida | `evidencias/S4/hardening-producao-backend.md` e evidencia Flutter `android-cleartext.md` |
| S5 - Sessao e biometria | Concluida no escopo seguro atual | Evidencia Flutter `sessao-biometria-sem-senha.md` |
| S6 - Segredos | Concluida | `evidencias/S6/gestao-segredos-backend.md` e evidencia Flutter S6 |
| S7 - Pipeline e gates | Implementada, execucao automatica bloqueada por infra | `evidencias/S7/pipeline-security-gates-backend.md`, pipeline versionado |
| S8 - Verificacao final | Parcial concluida com bloqueios formais | Matrizes S8, threat models S8, regressao S8 |

## Gates NO-GO

| Gate | Estado | Evidencia | Decisao |
|---|---|---|---|
| NO-GO-01 Senha persistida para biometria | Sem regressao local | Teste/gate Flutter S5-S8 | Nao bloqueia continuidade |
| NO-GO-02 Token em URL/log | Sem regressao local | S2 e `security_regression.sh` | Nao bloqueia continuidade |
| NO-GO-03 Bypass/BOLA/BFLA/BOPLA | Parcial | Spring Security e testes locais | Condicionado a teste multiusuario/DAST |
| NO-GO-04 Cleartext em release | Sem regressao Android local | Teste Android cleartext | iOS pendente |
| NO-GO-05 Segredo real exposto | Sem evidencia no escopo mobile/common | Secret scan S7/S8 | Historico/rotacao dependem de processo |
| NO-GO-06 CVE High/Critical exploravel | Parcial | Inventario de dependencias S7 | Condicionado a SCA formal |
| NO-GO-07 Build prod debug/default inseguro | Parcial | S4/S6 e build homolog | Release assinado/prod-like pendente |
| NO-GO-08 DAST com bypass critico | Bloqueado | Sem homologacao autorizada | NO-GO para declaracao final irrestrita |
| NO-GO-09 Matriz critica sem evidencia | Parcial | Matrizes S8 | Itens dinamicos marcados bloqueados |
| NO-GO-10 SBOM ausente | Parcial | Inventarios S7 | SBOM CycloneDX final pendente |

## Testes e verificacoes executadas

Backend:

```bash
bash restapi/mobile-service/scripts/security/secret_scan.sh
bash restapi/mobile-service/scripts/security/security_regression.sh
mvn -q -pl restapi-common,mobile-service test
mvn -q -pl restapi-common,mobile-service -DskipTests package
```

Flutter:

```bash
bash scripts/security/secret_scan.sh
bash scripts/security/security_regression.sh
flutter analyze
flutter test
flutter build apk --debug --flavor homolog -t lib/main.dart
```

Resultados detalhados:

- `evidencias/S8/regressao-final-backend.md`
- `app_mppb_flutter/seguranca/evidencias/S8/regressao-final-mobile.md`

## Bloqueios formais

| Bloqueio | Owner sugerido | Condicao de desbloqueio |
|---|---|---|
| Runner GitLab ausente | Infra/DevOps | Runner Docker ou pipeline adaptado para shell institucional |
| DAST homologacao | Segurança/QA/DevOps | Ambiente autorizado, usuarios de teste e escopo aprovado |
| iOS/IPA | Mobile/Infra Apple | Certificado, provisioning e aparelho/simulador controlado |
| SCA/SBOM formal | Segurança/DevOps | Ferramenta institucional e retencao dos artefatos |
| SAST dedicado | Segurança/DevOps | Definir ferramenta institucional e integrar ao pipeline |
| Testes dinamicos mobile | Segurança/Mobile | Laboratorio com aparelho/emulador e autorizacao |
| Rotacao real de segredos | Owners dos sistemas integrados | Processo/cofre institucional e janela de rotacao |
| Rate limiting distribuido | Infra/Backend | Confirmar topologia; se houver multiplas instancias, usar gateway/proxy/Redis/solucao institucional |

## Riscos residuais aceitos temporariamente

| Risco | Severidade | Justificativa | Prazo recomendado de revisao |
|---|---|---|---|
| BOLA/BFLA multiusuario ainda nao testado em homologacao | Alta | Exige massa e credenciais de usuarios distintos | Antes da promocao final |
| DAST ativo nao executado | Alta | Nao ha homologacao autorizada neste ciclo | Antes da promocao final |
| SBOM/SCA formal ausente | Media/Alta | Pipeline local gera inventario, nao triagem CVE formal | Proximo ciclo de release |
| SAST dedicado ausente | Media/Alta | Scripts locais de regressao nao substituem ferramenta SAST | Antes de gate final de release |
| iOS sem validacao | Media | Ambiente Apple nao disponivel nesta execucao | Antes de release iOS |
| Biometria segura futura sem contrato backend | Media | Fluxo inseguro foi desativado; funcionalidade segura futura deve ser desenhada | Antes de reabilitar biometria |
| Rate limit/captcha em topologia real nao testado | Media | Teste ativo poderia afetar ambiente sem autorizacao | Homologacao com carga controlada |
| Rate limit distribuido nao comprovado | Media | Estado atual distingue controle local/em memoria de controle distribuido | Revisar com Infra antes de producao |

## Conclusao

O trabalho S1-S8 elevou o projeto de um estado com riscos criticos conhecidos para uma base com controles implementados, regressao local, evidencias e rastreabilidade. A decisao tecnica atual permite seguir com o desenvolvimento e preparar homologacao segura, mas nao substitui DAST/pentest, SCA formal e validacao dinamica antes de uma declaracao final de prontidao de producao.

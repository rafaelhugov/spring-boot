# S8 - Threat model por fluxo sensivel

Data: 2026-09-24

Escopo: `mobile-service`, `restapi-common` e integrações chamadas pelo app MPPB+.

Metodo: STRIDE simplificado + abuse cases. Cada fluxo liga ativo, trust boundary, ameaca, mitigacao existente, teste/evidencia e risco residual.

## TM-01 - Autenticacao, sessao e biometria

| Item | Detalhe |
|---|---|
| Ativos | Senha institucional, token de acesso, login, matricula, CPF, futuro vinculo biometrico de dispositivo. |
| Atores | Usuario legitimo, usuario com aparelho compartilhado, atacante com token vazado, atacante com acesso local ao aparelho. |
| Trust boundaries | App Flutter -> auth-api; app Flutter -> mobile-service; mobile-service -> auth-api. |
| Ameacas | Token em URL/log; replay de token; senha persistida; biometria local criando sessao sem servidor; troca de usuario mantendo estado antigo; token expirado aceito por falha de validacao externa. |
| Mitigacoes existentes | S2 remove token de URL; S3 centraliza Bearer/Spring Security; S5 remove gravacao de `saved_password`; S7 bloqueia regressao de token/senha. |
| Testes/evidencias | `test/security/biometric_password_storage_test.dart`; `scripts/security/security_regression.sh`; `S8-mobile-masvs-mastg.md`; `S8-backend-asvs-api-top10.md`. |
| Risco residual | Revogacao/refresh e biometria segura dependem de contrato backend/auth-api ainda nao implementado. Testes de replay/expiracao exigem ambiente integrado. |
| Decisao | Sem bloqueador local; liberar apenas com risco residual documentado ate existir credencial biometrica revogavel. |

## TM-02 - Contracheque

| Item | Detalhe |
|---|---|
| Ativos | Competencias, remuneracao, descontos, PDF de contracheque. |
| Atores | Usuario autenticado, usuario tentando acessar competencia/arquivo de outro usuario, atacante com token. |
| Trust boundaries | App -> mobile-service; mobile-service -> bases/servicos internos de RH/financeiro. |
| Ameacas | BOLA por competencia/identidade; PDF cacheado ou compartilhado com token; vazamento em logs; enumeracao de competencias; cache local indevido. |
| Mitigacoes existentes | Endpoints privados exigem Bearer; usuario/matricula resolvidos no backend; token nao trafega em URL; testes de apresentacao e providers no Flutter. |
| Testes/evidencias | `ContrachequeController` protegido por SecurityConfig; matriz API-01/API-06; testes Flutter `contracheque_apresentacao_test.dart`; regressao S8 local. |
| Risco residual | Falta teste multiusuario com token A tentando dados/PDF de B; falta validacao de cache/no-store em resposta PDF e teste em aparelho. |
| Decisao | Parcialmente coberto; BOLA/PDF devem entrar no DAST/teste autenticado em homologacao. |

## TM-03 - Carteira funcional

| Item | Detalhe |
|---|---|
| Ativos | Foto, dados funcionais, assinatura, QR/hash publico, dados offline da carteira. |
| Atores | Usuario autenticado, terceiro com hash/URL publica, atacante com aparelho comprometido. |
| Trust boundaries | App -> mobile-service; mobile-service -> JCR/Google Wallet; endpoint publico de foto/logo. |
| Ameacas | Foto publica por hash permitir enumeracao; acesso horizontal a dados/foto; cache offline com PII; falsificacao/tampering visual; screenshot/compartilhamento indevido. |
| Mitigacoes existentes | Dados/foto autenticados fora de rotas publicas; token nao vai em URL; segredos JCR/Wallet fora do repo; cache vinculado ao usuario no app. |
| Testes/evidencias | `CarteiraServiceTest`; testes Flutter `usuario_carteira_test.dart`; S6 gestao de segredos; matriz MOB-STO/MOB-PLA. |
| Risco residual | Foto publica por MD5 precisa avaliacao de privacidade e rate limit; validacao de cache/screenshot exige aparelho. |
| Decisao | Sem bloqueador local; manter rota publica de foto sob revisao e DAST de enumeracao. |

## TM-04 - Concessoes, afastamentos e RH

| Item | Detalhe |
|---|---|
| Ativos | Concessoes, ausencias, solicitacoes, operacoes de cancelamento/adiamento/suspensao/retorno. |
| Atores | Usuario autenticado, usuario tentando operar solicitacao alheia, integracao MPVirtual/RH. |
| Trust boundaries | App -> mobile-service; mobile-service -> MPVirtual/RH; token/login encaminhado conforme `mpvirtual.recursos-humanos.auth-mode`. |
| Ameacas | BOLA/BFLA em `concessaoId`/`solicitacaoId`; replay/duplicidade de operacao; mass assignment em DTO; resposta externa maliciosa; logs com PII/corpo remoto. |
| Mitigacoes existentes | Endpoints exigem autenticacao; DTOs testados; operacoes passam por servico backend; logs sanitizados em falhas conhecidas. |
| Testes/evidencias | `ConcessaoFolgaControllerTest`; `OperacaoSolicitacaoRequestDtoTest`; testes Flutter de card/repository/models; matriz API-03/API-10. |
| Risco residual | Falta teste negativo multiusuario por objeto; falta teste de concorrencia/replay e payload extra em homologacao. |
| Decisao | Parcial; nao promover sem testar IDs/operacoes de outro usuario em homologacao. |

## TM-05 - Pagamentos

| Item | Detalhe |
|---|---|
| Ativos | CPF resolvido do usuario, exercicios e dados de pagamento. |
| Atores | Usuario autenticado, atacante com token de outro usuario. |
| Trust boundaries | App -> mobile-service; mobile-service -> fonte interna de pagamentos. |
| Ameacas | Acesso horizontal por CPF/login incorreto; enumeracao por exercicio; vazamento de valores em log; cache local/resposta excessiva. |
| Mitigacoes existentes | Autenticacao centralizada; token em header; backend resolve identidade do usuario. |
| Testes/evidencias | Inventario S1; matriz API-01/API-06; regressao S7/S8 contra token em query. |
| Risco residual | Falta teste negativo com token de outro usuario e exercicio inexistente/fora de escopo. |
| Decisao | Parcial; DAST autenticado deve incluir pagamentos. |

## TM-06 - Notificacoes e FCM

| Item | Detalhe |
|---|---|
| Ativos | Token FCM, identificador de instalacao, payload de notificacao, centro de notificacoes. |
| Atores | Usuario autenticado, atacante com payload malicioso, Firebase/FCM. |
| Trust boundaries | App -> Firebase; app -> mobile-service; mobile-service -> Firebase Admin. |
| Ameacas | Registrar token FCM para matricula indevida; token bruto em log; payload com PII/segredo; navegacao arbitraria; desativacao de dispositivo de outro usuario. |
| Mitigacoes existentes | Registro exige Bearer; backend usa matricula autenticada; logs usam tamanho/hash prefix; Flutter aceita payload tecnico permitido e bloqueia rota arbitraria. |
| Testes/evidencias | `DispositivoNotificacaoControllerTest`; `DispositivoFcmServiceTest`; `push_notification_service_test.dart`; `push_navigation_service_test.dart`; `device_registration_*_test.dart`. |
| Risco residual | Validar payload real no Firebase/homologacao e desativacao cross-user em teste integrado. |
| Decisao | Cobertura local forte; falta validacao integrada. |

## TM-07 - Consulta publica e captcha

| Item | Detalhe |
|---|---|
| Ativos | Disponibilidade da API publica, dados publicos de processos, captcha token/valor. |
| Atores | Cidadao, scraper, atacante tentando enumeracao/DoS leve. |
| Trust boundaries | Cliente publico -> mobile-service; mobile-service -> repositorio de processos. |
| Ameacas | Bypass de captcha; scraping massivo; rate limit em memoria falhar com multiplas instancias; enumeracao de IDs; consumo excessivo. |
| Mitigacoes existentes | Captcha de uso unico; rate limit simples por IP; detalhes exigem captcha. |
| Testes/evidencias | `ProcessoConsultaServiceTest`; `ProcessoDetalhesRepositoryTest`; inventario publico S1. |
| Risco residual | Rate limit precisa validação em topologia real; abuso/carga controlada e bypass de captcha exigem homologacao. |
| Decisao | Parcial; nao executar teste ativo fora de ambiente autorizado. |

## TM-08 - Integracoes externas

| Item | Detalhe |
|---|---|
| Ativos | Auth-api, MPVirtual/RH, JCR, Firebase Admin, Google Wallet, credenciais tecnicas. |
| Atores | Servicos internos, indisponibilidade externa, resposta maliciosa/incompleta, operador com segredo. |
| Trust boundaries | mobile-service -> sistemas internos/terceiros; arquivos/variaveis de segredo -> runtime. |
| Ameacas | Credencial tecnica exposta; timeout/esgotamento; resposta externa com dados inesperados; falha em cascata; unsafe consumption of APIs. |
| Mitigacoes existentes | S6 externaliza segredos; `.gitignore` reforcado; testes de repositorios/services; secret scan S7/S8. |
| Testes/evidencias | `FirebaseConfigTest`; `FcmSenderServiceTest`; `ProtocoloConsultaRepositoryTest`; `ConcessaoFolgaRemoteRepositoryTest`; S6/S7/S8. |
| Risco residual | Testes de timeout, circuit breaker, resposta maliciosa e rotacao real de credenciais dependem de homologacao/infra. |
| Decisao | Parcial; registrar como risco operacional ate testes integrados adversariais. |

## Conclusao

Nenhum novo bloqueador critico local foi identificado. Os maiores riscos residuais estao concentrados em testes integrados que exigem homologacao autorizada: BOLA multiusuario, DAST autenticado, abuso/rate limit, validacao de cache/headers de PDFs e testes dinamicos mobile em aparelho real.

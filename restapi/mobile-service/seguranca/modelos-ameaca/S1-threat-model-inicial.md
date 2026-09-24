# S1-T07 - Threat model inicial

Metodo: STRIDE simplificado + fluxos de abuso. Este modelo sera detalhado em S8.

## TM-01 - Autenticacao, sessao e biometria

Ativos:
- Senha institucional.
- `auth_token`.
- Credencial biometrica futura.
- Identidade login/matricula/CPF.

Ameacas:
- Roubo de token por query string, logs ou URL compartilhada.
- Reuso de senha persistida em `saved_password`.
- Confusao de identidade ao trocar usuario.
- Sessao antiga continuar valida apos logout se nao houver revogacao servidor.
- Biometria local ser tratada como autenticacao suficiente sem validacao backend.

Mitigacoes planejadas:
- S2 remove token de URL.
- S3 centraliza autenticacao/autorizacao.
- S5 substitui `saved_password` por credencial de dispositivo revogavel.

## TM-02 - Contracheque

Ativos:
- Remuneracao, descontos, PDF.

Ameacas:
- URL de PDF com token vazar.
- Cache/download externo sem controle.
- BOLA por competencia/identidade se validacao falhar.
- Logs de erro com payload sensivel.

Mitigacoes planejadas:
- S2: PDF autenticado sem token em URL.
- S3: autorizacao por usuario autenticado.
- S8: testes de cache/download e BOLA.

## TM-03 - Carteira funcional

Ativos:
- Foto, dados funcionais, QR/hash publico, carteira offline.

Ameacas:
- Foto autenticada com token em URL.
- Foto publica por MD5 permitir enumeração/privacidade indevida.
- Cache offline acessivel se dispositivo comprometido.
- Screenshot/compartilhamento indevido.

Mitigacoes planejadas:
- S2: foto autenticada por Bearer ou URL temporaria curta.
- S8: avaliar foto publica por hash, cache, screenshot e storage.

## TM-04 - Concessoes/afastamentos/RH

Ativos:
- Dados funcionais, ausencias, solicitacoes e operacoes.

Ameacas:
- BOLA/BFLA em `concessaoId`/`solicitacaoId`.
- Repeticao/replay de operacoes.
- Token repassado ao MPVirtual.
- Logs com corpo remoto contendo PII.

Mitigacoes planejadas:
- S2: token em header.
- S3: autorizacao por objeto.
- S8: testes de abuso, concorrencia e logs.

## TM-05 - Pagamentos

Ativos:
- CPF e dados de pagamento.

Ameacas:
- Query token vazar.
- Acesso horizontal por falha login->CPF.
- Enumeracao por exercicio.

Mitigacoes planejadas:
- S2: Bearer.
- S3: autorizacao central e testes negativos.

## TM-06 - Notificacoes/FCM

Ativos:
- Token FCM, rotas de push, centro de notificacoes.

Ameacas:
- Registro de token para matricula indevida.
- Payload FCM com segredo/PII.
- Navegacao arbitraria por payload malicioso.

Controles atuais:
- Registro usa Bearer manual.
- Testes de Flutter cobrem navegacao arbitraria e payload tecnico permitido.
- Backend loga hash prefix/tamanho, nao token bruto.

## TM-07 - Consulta publica/captcha

Ativos:
- Dados publicos de processos e disponibilidade.

Ameacas:
- Scraping/automacao.
- Bypass de captcha.
- Rate limit em memoria nao funcionar em multiplas instancias.

Mitigacoes planejadas:
- S8: testar abuso e topologia real.

## TM-08 - Integracoes externas

Ativos:
- auth-api, MPVirtual/RH, JCR, Firebase, Google Wallet.

Ameacas:
- Resposta externa maliciosa/incompleta.
- Timeout/esgotamento.
- Credencial tecnica exposta.
- Falha em cascata.

Mitigacoes planejadas:
- S6: segredos.
- S7: pipeline/SCA/SBOM.
- S8: unsafe consumption of APIs.


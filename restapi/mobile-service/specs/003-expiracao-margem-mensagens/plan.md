# Plano de Implementação: Margem Mínima de Expiração (`003-expiracao-margem-mensagens`)

- `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/service/MensagemEnvioService.java`: valida `dataExpiracao.isBefore(baseEnvio.plusHours(1))`.
- `restapi/mobile-service/src/test/java/mppb/restapi/mobile/feature/mensagem/MensagemEnvioTest.java`: adiciona teste unitário de margem mínima de 1 hora.

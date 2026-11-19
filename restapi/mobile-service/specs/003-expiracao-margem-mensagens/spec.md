# Especificação de Funcionalidade: Validação de Margem Mínima de Expiração após Envio/Agendamento (`mobile-service`)

**Branch da Feature**: `feat/expiracao-margem-mensagens`  
**Criado em**: 2026-11-19  
**Status**: Aprovado para Implementação  
**Subprojeto**: `restapi/mobile-service`  
**Feature Base**: `001-mensagens-avisos`

## Cenários de Usuário e Testes *(obrigatório)*

### História de Usuário 1 - Garantir Tempo Mínimo de Visibilidade Antes da Expiração (Priority: P1)

Como administrador enviando ou agendando um comunicado institucional, o backend **não** deve permitir que `dataExpiracao` seja anterior ou igual à data de envio (imediato ou agendado), exigindo margem mínima de **1 hora** a partir da data efetiva de publicação (`dataBase = dataAgendamento != null && dataAgendamento.isAfter(agora) ? dataAgendamento : agora`).

## Requisitos *(obrigatório)*

- **FR-001**: `MensagemEnvioService.enviarOuAgendar` deve validar que, quando `dataExpiracao != null`, ela seja maior ou igual a `baseEnvio.plusHours(1)` (onde `baseEnvio` é `dataAgendamento` futuro ou `agora`), lançando `IllegalArgumentException` caso contrário.
- **FR-002**: `MensagemEnvioTest` deve conter teste unitário verificando a rejeição quando `dataExpiracao` for anterior ou menor que 1 hora após `dataAgendamento` / `agora`.

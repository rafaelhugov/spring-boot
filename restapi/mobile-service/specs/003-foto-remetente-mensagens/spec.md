# Spec 003 — Foto do Remetente nos Avisos e Mensagens

## Contexto
Para exibir a foto do usuário remetente na caixa de entrada e na tela de detalhe do aviso no aplicativo mobile (`app_mppb_flutter`), o backend (`mobile-service`) precisa expor a matrícula do remetente na listagem/detalhe e fornecer endpoint para recuperação da foto por matrícula.

## Requisitos Funcionais
- **RF-01**: `MensagemRecebidaResponse` deve incluir o campo `remetenteMatricula`.
- **RF-02**: `CarteiraService` deve expor `getBytesFotoPorMatricula(String matricula)` com suporte tanto às contas sintéticas (`900001` e `900002`) quanto à base funcional real (`pessoafisica` + `servidor` + `ged.arquivo`/JCR) usando query parametrizada.
- **RF-03**: `MensagemDestinatarioController` deve expor `GET /api/mensagens/remetentes/{matricula}/foto` retornando `image/jpeg` (com `Cache-Control: max-age=3600`) ou `404 Not Found` quando não houver foto disponível.

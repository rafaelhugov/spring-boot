# Plan 005 — Isolamento do Painel por Usuário Remetente

1. Adicionar `findByRemetenteMatriculaAndArquivadaRemetenteOrderByDataRegistroDesc` em `MensagemAvisoRepository.java`.
2. Extrair helper de resolução de matrícula do usuário autenticado (suportando sintéticos + `PerfilUsuarioRepository`) e aplicar o filtro/validação de propriedade em `MensagemAcompanhamentoService.java`, `MensagemAcompanhamentoController.java`, `MensagemEnvioService.java` e `MensagemRemetenteController.java`.
3. Atualizar e rodar `mvn -pl mobile-service -Dtest="*Mensagem*" test`.

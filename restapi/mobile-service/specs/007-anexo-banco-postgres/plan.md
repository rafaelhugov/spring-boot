# Plan: 007 - Armazenamento de Anexos em PostgreSQL (BYTEA)

- Criar script SQL `src/main/resources/sql/2026-11-24_mensagemanexo_bytea.sql` com `ALTER TABLE diariooficial.mensagemanexo ADD COLUMN IF NOT EXISTS conteudobytes BYTEA, ADD COLUMN IF NOT EXISTS thumbnailbytes BYTEA`.
- Atualizar `MensagemAnexo.java` com `conteudoBytes` e `thumbnailBytes`.
- Atualizar `MensagemStorageService.java` para retornar `conteudoBytes` no `armazenar(...)` sem gravar em disco, e servir `ByteArrayResource` a partir do banco (com fallback legado para disco se aplicável).
- Atualizar `MensagemRemetenteController.java` e `MensagemDestinatarioController.java`.
- Adicionar testes unitários validando o armazenamento e leitura em memória/banco sem escrita em disco.

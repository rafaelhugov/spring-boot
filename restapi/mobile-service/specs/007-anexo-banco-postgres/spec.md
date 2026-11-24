# Feature Spec: 007 - Armazenamento de Anexos em PostgreSQL (BYTEA)

## Problema
Os anexos de avisos (`imagem`, `vídeo`, `áudio`, `documento`) estavam sendo gravados em disco local (`MENSAGENS_STORAGE_PATH`), exigindo volume persistente nos pods/servidores do MPPB.

## Requisitos
1. Salvar os bytes dos anexos diretamente na tabela `diariooficial.mensagemanexo` (`conteudobytes BYTEA`, `thumbnailbytes BYTEA`) sem gravar arquivos no sistema de arquivos do servidor.
2. Manter compatibilidade de leitura caso algum anexo legado tenha sido salvo anteriormente em disco.
3. Garantir que listagens de avisos não carreguem desnecessariamente os bytes dos anexos e que o streaming (`ResourceRegion` / `ByteArrayResource`) continue funcionando normalmente.

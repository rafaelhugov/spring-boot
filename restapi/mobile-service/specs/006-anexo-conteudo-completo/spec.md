# Feature Spec: 006 - Retorno Completo de Anexo sem Header Range

## Problema
`MensagemDestinatarioController.resolverRegiao` limitava o tamanho da resposta a `CHUNK_SIZE_PADRAO` (1 MB) mesmo quando a requisição não enviava header `Range`, truncando imagens e arquivos maiores que 1 MB.

## Requisitos
1. Quando `Range` estiver ausente, retornar o arquivo completo (`new ResourceRegion(resource, 0, tamanhoTotal)`).
2. Quando `Range` estiver presente, manter o streaming em chunks de `CHUNK_SIZE_PADRAO` com `206 Partial Content`.

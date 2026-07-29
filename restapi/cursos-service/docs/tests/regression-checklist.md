# Checklist de Regressao

- Login, logout, token expirado e redirecionamento de rota.
- Usuario sem permissao recebe `AcessoRestritoView`.
- Dashboard carrega estatisticas, filtros e acoes.
- Solicitacao: rascunho, edicao, submissao, PDF, protocolo, habilitar edicao e conversao.
- Curso: criar, editar, listar, remover, detalhe e status.
- Upload/download: programacao GED, arquivos comprobatorios, certificado-template.
- Ministrante: busca servidor RH, cria externo, associa, altera papel e remove associacao.
- Participante: busca, adiciona interno/externo, altera situacao, remove, exporta Excel.
- Inscricao publica: lista curso, detalhe por hash, verifica participante, inscreve externo.
- Inscricao interna: confirmar inscricao e listar meus cursos.
- PCD/acessibilidade: checklist, necessidade especial e persistencia.
- Aulas: criar, editar, remover, quantidade de chamadas.
- Frequencia: QR, chamada inicio/fim, encerrar, registro publico, frequencia manual.
- Documentos: lista de presenca, DRH, corregedoria, consolidado e certificado.
- Manuais: solicitante publico e administrativo autenticado.
- Responsividade: mobile 360px, tablet, desktop.
- Acessibilidade: foco visivel, labels, contraste, teclado.

## Fase 2A - Cursos API

Data: 2026-07-29.

Branch: `test/phase-2a-cursos-api`.

Status: pronto para MR, sem commit/push/merge executado.

Checklist confirmado:

- API administrativa de Cursos usa o endpoint real `/api/cursos`.
- Sem autenticacao retorna `401`.
- Token invalido retorna `401`.
- Usuario com apenas `cursos.acesso` retorna `403`.
- Usuario com `cursos.manter.cursos` acessa os fluxos administrativos testados.
- Listagem cobre paginacao valida, sort valido, filtro por titulo, status valido e SQL injection parametrizada.
- Listagem rejeita `page < 0` e `size < 1`.
- Detalhe cobre curso inexistente e os estados reais `EM_CONSTRUCAO`, `PUBLICADO`, `FINALIZADO`.
- Criacao valida retorna `201`, define `EM_CONSTRUCAO` e gera `hashInscricao`.
- Criacao rejeita titulo ausente, carga horaria invalida, percentual invalido, publico alvo vazio, campo desconhecido e `id` enviado pelo cliente.
- Edicao valida preserva status, hash e data de criacao.
- Edicao rejeita curso inexistente e id divergente no payload sem alterar o banco.
- Exclusao remove curso sem dependencia.
- Exclusao de curso inexistente retorna `404`.
- Exclusao com aula/inscricao/participante retorna `409` sanitizado e nao remove parcialmente.
- Comprovantes cobrem listagem, upload valido em curso finalizado, rejeicoes e remocao.
- Respostas de erro cobertas nao expoem SQL, stack trace, constraint, CPF, token ou nomes de tabela nos asserts aplicaveis.
- `CursoService.update` nao recalcula dados derivados de aulas em PUT comum.
- `AulaService.createAula`, `updateAula` e `deleteAula` continuam chamando `recalcularDadosCurso`.

Pendencias registradas:

- `INSCRICOES_ENCERRADAS` nao existe em `StatusCursoEnum`.
- Nao ha endpoint dedicado de publicacao, finalizacao, regressao de status ou encerramento de inscricoes.
- Testes funcionais profundos de recalculo por aula ficam para fase de Aulas/Frequencia.
- Sort invalido, size excessivo, filtro muito longo, caracteres especiais e regras de imagem de cabecalho/template/certificados podem ser formalizados em fases posteriores.

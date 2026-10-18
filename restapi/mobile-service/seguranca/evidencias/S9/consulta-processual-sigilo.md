# S9 - Consulta processual publica e sigilo

Data: 2026-10-16

Escopo: endpoints publicos de consulta, filtros e detalhes de processos do `mobile-service`.

## Resultado

A regra publica aceita somente processo raiz, nao excluido e sem sigilo ativo. A unica excecao e quando o ultimo movimento de sigilo representa desclassificacao (`cod_movimento = 920360`). Referencia de sigilo ausente ou inconsistente e bloqueada (fail-closed).

Foi encontrado e corrigido um risco lateral: o historico de um processo publico podia selecionar o resumo de um documento anexado que tivesse sigilo proprio. A consulta agora elimina o movimento que referencia documento com sigilo ativo, alem de eliminar movimentos excluidos e movimentos filhos.

O mesmo predicado obrigatorio foi aplicado de forma independente a:

- lista de resultados;
- origem dos filtros de comarca;
- cabecalho de detalhes por ID;
- interessados;
- movimentos.

## Matriz adversarial

| Vetor | Controle verificado | Resultado |
|---|---|---|
| SQL injection (`OR 1=1`, comentario e comando adicional) | valores continuam em parametros JDBC; predicado de sigilo permanece no SQL | Bloqueado |
| IDOR/enumeracao por ID no endpoint de detalhes | cabecalho exige processo publico antes de qualquer consulta filha | Bloqueado |
| Chamada futura/direta das consultas filhas | interessados e movimentos repetem a validacao do processo | Bloqueado |
| Interessado marcado como sigiloso | `i.sigiloso = false` e registro nao excluido | Bloqueado |
| Documento anexado com sigilo proprio | movimento exige documento sem sigilo ativo ou desclassificado | Bloqueado |
| Movimento excluido ou aninhado | filtros de exclusao e de movimento raiz | Bloqueado |
| Vazamento de existencia via lista de comarcas | filtro e formado somente a partir de processos publicos | Bloqueado |
| Referencia de movimento de sigilo quebrada | somente `EXISTS` com desclassificacao libera; erro nao libera | Bloqueado |
| Processo excluido, documento ou subprotocolo | tipo, raiz e data de exclusao sao validados em cada leitura | Bloqueado |
| CAPTCHA valido seguido de ID sigiloso/inexistente | mesma resposta generica e nenhuma consulta filha | Bloqueado |

## Testes automatizados

Comando:

```bash
mvn -q -DskipTests=false \
  -Dtest=ProtocoloConsultaRepositoryTest,ProcessoConsultaServiceTest,ProcessoDetalhesRepositoryTest,ProcessoDetalhesServiceSecurityTest \
  test
```

Resultado: 10 testes, 0 falhas, 0 erros, 0 ignorados.

Compilacao adicional:

```bash
mvn -q -DskipTests compile
```

Resultado: passou.

## Validacao no ATENDIMENTO

A verificacao foi executada em transacao `READ ONLY`, sem retornar identificadores ou dados pessoais. Em amostra de 1.000 processos raiz com marcador de sigilo:

- 998 tinham sigilo ativo e foram bloqueados;
- 2 estavam desclassificados e foram considerados publicos;
- 0 tinham referencia invalida;
- 0 processos com sigilo ativo foram aceitos pela regra publica.

A tentativa inicial de contagem integral foi cancelada por `statement_timeout` de 45 segundos devido ao volume da base. Nenhuma escrita foi realizada. Portanto, a comprovacao combina amostra real, inspecao estrutural das consultas e regressao automatizada; nao declara varredura integral de todas as linhas do ambiente.

## Arquivos principais

- `core/security/ProcessoPublicoSql.java`
- `feature/processoconsulta/repository/ProtocoloConsultaRepository.java`
- `feature/processodetalhe/repository/ProcessoDetalhesRepository.java`
- `ProtocoloConsultaRepositoryTest.java`
- `ProcessoDetalhesRepositoryTest.java`
- `ProcessoDetalhesServiceSecurityTest.java`

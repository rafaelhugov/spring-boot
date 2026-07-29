# Casos de Teste Funcionais

## Padrao de caso

Campos obrigatorios: ID, modulo, objetivo, pre-requisitos, passos, dados de teste, resultado esperado, prioridade, criticidade, tipo, responsavel e dependencias.

| ID | Modulo | Funcionalidade/regra | Cenarios positivos | Negativos/extremos | Validacoes obrigatorias | Tipo | Pri/Crit |
|---|---|---|---|---|---|---|---|
| CT-CUR-001 | Cursos | Criar curso | Curso valido com tipo, tutora, modalidade, publico alvo | titulo vazio, carga horaria invalida, percentual >100, publico alvo vazio | `titulo`, `tipoEvento`, `entidadeTutora`, `modalidade`, `publicosAlvo`, `HHHH:MM:SS` | Unit/API/E2E | P0/Alta |
| CT-CUR-002 | Cursos | Editar curso | Atualizar campos mantendo ID | ID divergente, desconhecido, payload extra | `FAIL_ON_UNKNOWN_PROPERTIES`, grupo Update | API/Regressao | P1/Alta |
| CT-CUR-003 | Cursos | Arquivos comprobatorios | Upload e listagem | arquivo >5MB, MIME perigoso, curso inexistente | multipart, tamanho, autorizacao | API/Sec | P0/Alta |
| CT-CUR-004 | Cursos | Ministrantes do curso | Associar interno/externo | duplicidade, papel inexistente, IDOR entre cursos | tipo, servidor, CPF, papel | Unit/API/E2E | P0/Alta |
| CT-CUR-005 | Cursos | Participantes do curso | Inscricao admin e export Excel | CPF invalido, duplicidade, situacao invalida | tipo participante, email, CPF | API/E2E | P0/Alta |
| CT-CUR-006 | Aulas | Criar aula | Aula com data/hora valida | hora fim antes inicio, curso inexistente | descricao, data, horaInicio, horaFim | Unit/API | P0/Alta |
| CT-FRQ-001 | Frequencia | Gerar chamada | Token unico por aula/tipo | tipo duplicado, aula inexistente | `INICIO/FIM`, aberta=true | Unit/API/E2E | P0/Alta |
| CT-FRQ-002 | Frequencia | Registrar presenca publica | CPF inscrito durante aula | CPF invalido, fora do horario, chamada fechada, concorrencia dupla | CPF 11 digitos, token/aula | API/Concorrencia | P0/Critica |
| CT-SOL-001 | Solicitacoes | Criar rascunho/submetida | Dados minimos validos | sem agenda, estimativa 0, ministrante sem foto/curriculo | nomeCurso, estimativa, publico, modalidade, tipo | Unit/API/E2E | P0/Alta |
| CT-SOL-002 | Solicitacoes | Protocolar | Solicitação valida gera protocolo/minuta | status incorreto, MPVirtual indisponivel | transacao e idempotencia | Integracao/API | P0/Critica |
| CT-SOL-003 | Solicitacoes | Converter para curso | Protocolada vira curso | ja convertida, dados incompletos | carga horaria das agendas, municipio/local | Unit/API | P0/Alta |
| CT-INS-001 | Inscricao publica | Inscrever externo | Novo participante ou existente | CPF duplicado, email invalido, hash invalido, XSS em nome | CPF, email, PCD | API/E2E/Sec | P0/Critica |
| CT-INS-002 | Inscricao interna | Inscrever usuario logado | Inscricao com dados do token | sem permissao, token expirado | `cursos.acesso` | API/E2E | P0/Alta |
| CT-PCD-001 | Acessibilidade | Checklist PCD | Marca PCD e necessidade | necessidade longa/script | mensagens e persistencia | Componente/API | P1/Media |
| CT-MIN-001 | Ministrantes | Buscar/criar ministrante | Mescla RH e externos | tipo invalido, CPF vazio, servidor ausente | interno exige servidor; externo normaliza CPF/nome | Unit/API | P0/Alta |
| CT-CAD-001 | Entidade tutora | CRUD | Listar/criar/editar/remover | nome vazio, remover em uso | nome obrigatorio | API/E2E | P1/Media |
| CT-DOC-001 | Documentos | Gerar PDFs | Presenca, DRH, corregedoria | curso sem dados, template ausente | content-type PDF, tamanho >0 | Integracao/API | P1/Alta |
| CT-CERT-001 | Certificado | Emitir certificado | Curso finalizado e participante/ministrante valido | curso nao finalizado, pessoa sem vinculo, template ausente | status FINALIZADO | Unit/API | P0/Alta |
| CT-DASH-001 | Dashboard | Estatisticas/listas | Usuario autorizado ve dados | sem permissao, filtros extremos | permissao e paginacao | API/E2E | P1/Media |
| CT-AUTH-001 | Router guard | Navegacao segura | Redireciona login/acesso restrito | rota segura sem permissao especifica | fail-safe no router | Unit/E2E/Sec | P0/Alta |


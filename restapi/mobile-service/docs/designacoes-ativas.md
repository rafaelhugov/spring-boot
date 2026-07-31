# Designações Ativas

## Objetivo

Exibir no MPPB+ as designações ativas do usuário autenticado a partir da view `diariooficial.mapa_por_membro_vw`, usando a matrícula obtida pelo token validado no `auth-api`.

## Fonte de dados

Banco remoto: ATENDIMENTO.

View somente leitura:

```sql
diariooficial.mapa_por_membro_vw
```

Colunas confirmadas no ATENDIMENTO:

- `designacao_id`: integer
- `servidor_matricula`: character varying
- `ativo`: text
- `designacao_tipo`: text
- `designacao_tipoletra`: character
- `funcao_nome`: character varying
- `orgao_nome`: character varying
- `orgao_execucao_nome`: character varying
- `designacao_data_inicial`: date
- `designacao_data_final`: date
- `diario_oficial_data_publicacao`: date
- `ato_numero`: character varying
- `tipocargo`: character
- `tipocargo_nome`: text

A chave primária de `diariooficial.designacao` confirmada no ATENDIMENTO é `id_efeito` (`integer`). A tabela de controle mantém a coluna `id_designacao`, conforme padrão solicitado, referenciando a chave primária da tabela `diariooficial.designacao`.

## Endpoint

```http
GET /api/designacoes/ativas?token={token}
```

A matrícula não é recebida do Flutter. O backend consulta o `auth-api`, obtém o login e resolve a matrícula em `autorizacao.usuario`.

## Primeiro acesso

Como o aplicativo autentica diretamente no `auth-api`, sem endpoint de login próprio no `mobile-service`, a inicialização individual é executada no primeiro acesso autenticado ao endpoint de designações:

1. cria o controle do usuário se ausente;
2. bloqueia a linha do controle com `FOR UPDATE`;
3. consulta as designações ativas da matrícula;
4. registra as existentes como `origemregistro = 'I'` e `statusnotificacao = 'NA'`;
5. marca o usuário como inicializado.

## Códigos

- `I = Inicialização`
- `N = Nova designação`
- `NA = Não aplicável`
- `PE = Pendente`
- `EN = Enviada`
- `ER = Erro`

## Classificação

Membros:

- `T = Titularidade`
- `S = Substituição Cumulativa`
- `F = Função`

Servidores, conforme dados reais encontrados na view:

- `L = Lotação`
- `F = Função`
- demais códigos ficam como designação temporária ou desconhecida sem suposição adicional.

## Scheduler

Configuração:

```properties
DESIGNACOES_NOTIFICACAO_ENABLED=false
DESIGNACOES_NOTIFICACAO_CRON=0 */15 * * * *
DESIGNACOES_NOTIFICACAO_BATCH_SIZE=100
DESIGNACOES_NOTIFICACAO_MAX_TENTATIVAS=3
DESIGNACOES_FCM_ENABLED=false
```

O scheduler processa apenas usuários inicializados em `diariooficial.designacaousuariocontrole`. Usuários que nunca acessaram a funcionalidade não são carregados globalmente.

## SQL

Script:

```text
src/main/resources/sql/2026-07-30_designacoes_ativas.sql
```

Cria:

- `diariooficial.designacaousuariocontrole`
- `diariooficial.designacaoconhecida`
- constraints de origem/status
- índice por inicialização
- índice por matrícula
- índice por status/tentativas
- índice por publicação

## Pendências de infraestrutura

A infraestrutura completa de FCM/tokens de dispositivo ainda não existe no `mobile-service`. A detecção de novas designações já registra as novidades como `N/PE`; o envio real deve ser ligado quando houver cadastro de tokens por usuário/dispositivo e credenciais Firebase externas ao código-fonte.

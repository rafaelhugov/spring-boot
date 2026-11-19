# Modelo de Dados: Mensagens e Avisos Institucionais (`mobile-service`)

**Referência**: [plan.md](./plan.md) | [spec.md](./spec.md)

## 1. Tabelas Próprias (PostgreSQL - `mobile-service`)

> Nota: Seguir o padrão de scripts SQL em `src/main/resources/sql/` do `mobile-service`.

```sql
CREATE TABLE IF NOT EXISTS diariooficial.mensagem_aviso (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(160),
    conteudo TEXT,
    prioridade VARCHAR(16) NOT NULL CHECK (prioridade IN ('NORMAL', 'IMPORTANTE', 'URGENTE')),
    modalidade VARCHAR(16) NOT NULL CHECK (modalidade IN ('BROADCAST', 'MULTICAST', 'UNICAST')),
    exige_ciencia BOOLEAN NOT NULL DEFAULT FALSE,
    status_publicacao VARCHAR(16) NOT NULL CHECK (status_publicacao IN ('AGENDADA', 'ENVIADA', 'CANCELADA')),
    criterios_json TEXT,
    remetente_matricula VARCHAR(20) NOT NULL,
    remetente_nome VARCHAR(180) NOT NULL,
    data_agendamento TIMESTAMP,
    data_envio TIMESTAMP,
    data_expiracao TIMESTAMP,
    arquivada_remetente BOOLEAN NOT NULL DEFAULT FALSE,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diariooficial.mensagem_destinatario (
    id BIGSERIAL PRIMARY KEY,
    mensagem_id BIGINT NOT NULL REFERENCES diariooficial.mensagem_aviso(id) ON DELETE CASCADE,
    destinatario_matricula VARCHAR(20) NOT NULL,
    destinatario_nome VARCHAR(180) NOT NULL,
    destinatario_setor VARCHAR(180),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'ENTREGUE', 'LIDO')),
    data_entrega TIMESTAMP,
    data_leitura TIMESTAMP,
    data_ciencia TIMESTAMP,
    arquivada_destinatario BOOLEAN NOT NULL DEFAULT FALSE,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mensagem_destinatario UNIQUE (mensagem_id, destinatario_matricula)
);

CREATE INDEX IF NOT EXISTS idx_msg_dest_matricula_status
    ON diariooficial.mensagem_destinatario (destinatario_matricula, arquivada_destinatario, status);

CREATE INDEX IF NOT EXISTS idx_msg_dest_mensagem_status
    ON diariooficial.mensagem_destinatario (mensagem_id, status);

CREATE TABLE IF NOT EXISTS diariooficial.mensagem_anexo (
    id BIGSERIAL PRIMARY KEY,
    mensagem_id BIGINT REFERENCES diariooficial.mensagem_aviso(id) ON DELETE CASCADE,
    categoria VARCHAR(16) NOT NULL CHECK (categoria IN ('IMAGEM', 'VIDEO', 'AUDIO', 'DOCUMENTO')),
    nome_original VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    tamanho_bytes BIGINT NOT NULL CHECK (tamanho_bytes > 0),
    duracao_segundos INTEGER,
    caminho_storage VARCHAR(500) NOT NULL,
    caminho_thumbnail VARCHAR(500),
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diariooficial.grupo_mensagem (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(300),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por_matricula VARCHAR(20) NOT NULL,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diariooficial.grupo_mensagem_membro (
    id BIGSERIAL PRIMARY KEY,
    grupo_id BIGINT NOT NULL REFERENCES diariooficial.grupo_mensagem(id) ON DELETE CASCADE,
    matricula VARCHAR(20) NOT NULL,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_grupo_mensagem_membro UNIQUE (grupo_id, matricula)
);
```

## 2. Consultas nas Tabelas Existentes de RH (Somente Leitura)

Baseadas em `PerfilUsuarioRepository.java`:
- `autorizacao.usuario u` (`login`, `matricula`, `excluido = false`)
- `servidor s` (`matricula`, `ativo = 'A'`, `excluido = false`, `id_pessoa`, `id_cargo`, `id_setor`)
- `pessoa p` (`id`, `nome`, `excluido = false`)
- `recursoshumanos.funcao f` (`id`, `nome`, `id_tipocargo`)
- `recursoshumanos.tipocargo tc` (`id`, `tipo`: `'M'` = Membro, `'S'` = Servidor)
- `setor st` (`id`, `nome`, `sigla`, `id_orgao`, `excluido = false`)
- `orgao o` (`id_pessoa`, `sigla`, `excluido = false`)

### Filtros suportados para `MULTICAST` dinâmico
- `tiposCargo`: `['M']`, `['S']` ou ambos
- `orgaoIds`: lista de `o.id_pessoa`
- `setorIds`: lista de `st.id`
- `cargoIds`: lista de `f.id`
- `grupoPersonalizadoIds`: lista de `grupo_mensagem.id` (resolvidos via `grupo_mensagem_membro` cruzado com `servidor.ativo = 'A'`)


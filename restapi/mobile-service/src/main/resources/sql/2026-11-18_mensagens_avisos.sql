-- Script padronizado pelo DBA e executado no banco de homologação (mobile-service)
-- Data: 2026-11-18

CREATE TABLE IF NOT EXISTS diariooficial.mensagemaviso (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(160),
    conteudo TEXT,
    prioridade VARCHAR(16) NOT NULL CHECK (prioridade IN ('NORMAL', 'IMPORTANTE', 'URGENTE')),
    modalidade VARCHAR(16) NOT NULL CHECK (modalidade IN ('BROADCAST', 'MULTICAST', 'UNICAST')),
    exigeciencia BOOLEAN NOT NULL DEFAULT FALSE,
    statuspublicacao VARCHAR(16) NOT NULL CHECK (statuspublicacao IN ('AGENDADA', 'ENVIADA', 'CANCELADA')),
    criteriosjson TEXT,
    remetentematricula VARCHAR(20) NOT NULL,
    remetentenome VARCHAR(180) NOT NULL,
    dataagendamento TIMESTAMP,
    dataenvio TIMESTAMP,
    dataexpiracao TIMESTAMP,
    arquivadaremetente BOOLEAN NOT NULL DEFAULT FALSE,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diariooficial.mensagemdestinatario (
    id BIGSERIAL PRIMARY KEY,
    mensagem_id BIGINT NOT NULL REFERENCES diariooficial.mensagemaviso(id) ON DELETE CASCADE,
    destinatariomatricula VARCHAR(20) NOT NULL,
    destinatarionome VARCHAR(180) NOT NULL,
    destinatariosetor VARCHAR(180),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'ENTREGUE', 'LIDO')),
    dataentrega TIMESTAMP,
    dataleitura TIMESTAMP,
    dataciencia TIMESTAMP,
    arquivadadestinatario BOOLEAN NOT NULL DEFAULT FALSE,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mensagem_destinatario UNIQUE (mensagem_id, destinatariomatricula)
);

CREATE INDEX IF NOT EXISTS idx_msg_dest_matricula_status
    ON diariooficial.mensagemdestinatario (destinatariomatricula, arquivadadestinatario, status);

CREATE INDEX IF NOT EXISTS idx_msg_dest_mensagem_status
    ON diariooficial.mensagemdestinatario (mensagem_id, status);

CREATE TABLE IF NOT EXISTS diariooficial.mensagemanexo (
    id BIGSERIAL PRIMARY KEY,
    mensagem_id BIGINT REFERENCES diariooficial.mensagemaviso(id) ON DELETE CASCADE,
    categoria VARCHAR(16) NOT NULL CHECK (categoria IN ('IMAGEM', 'VIDEO', 'AUDIO', 'DOCUMENTO')),
    nomeoriginal VARCHAR(255) NOT NULL,
    mimetype VARCHAR(120) NOT NULL,
    tamanhobytes BIGINT NOT NULL CHECK (tamanhobytes > 0),
    duracaosegundos INTEGER,
    caminhostorage VARCHAR(500) NOT NULL,
    caminhothumbnail VARCHAR(500),
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diariooficial.grupomensagem (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(300),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criadopormatricula VARCHAR(20) NOT NULL,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diariooficial.grupomensagemmembro (
    id BIGSERIAL PRIMARY KEY,
    grupo_id BIGINT NOT NULL REFERENCES diariooficial.grupomensagem(id) ON DELETE CASCADE,
    matricula VARCHAR(20) NOT NULL,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_grupo_mensagem_membro UNIQUE (grupo_id, matricula)
);

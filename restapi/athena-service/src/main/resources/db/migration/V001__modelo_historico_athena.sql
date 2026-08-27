CREATE SCHEMA IF NOT EXISTS athena;

CREATE TABLE athena.usuarioinstitucional (
    id UUID PRIMARY KEY,
    identificador VARCHAR(120) NOT NULL UNIQUE,
    nome VARCHAR(200) NOT NULL,
    login VARCHAR(120) NOT NULL UNIQUE,
    unidade VARCHAR(80) NOT NULL,
    cadastradoem TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE athena.projeto (
    id UUID PRIMARY KEY,
    identificador VARCHAR(80) NOT NULL UNIQUE,
    nome VARCHAR(200) NOT NULL,
    descricao TEXT NOT NULL,
    situacao VARCHAR(40) NOT NULL,
    criadoem TIMESTAMPTZ NOT NULL,
    criadoporid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    alteradoem TIMESTAMPTZ,
    alteradoporid UUID REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT
);

CREATE TABLE athena.vinculoprojeto (
    id UUID PRIMARY KEY,
    projetoid UUID NOT NULL REFERENCES athena.projeto(id) ON DELETE RESTRICT,
    usuarioid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    papel VARCHAR(60) NOT NULL,
    unidade VARCHAR(80) NOT NULL,
    inicioem TIMESTAMPTZ NOT NULL,
    fimem TIMESTAMPTZ,
    registradoporid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    UNIQUE (projetoid, usuarioid, papel, inicioem)
);

CREATE TABLE athena.documento (
    id UUID PRIMARY KEY,
    identificador VARCHAR(120) NOT NULL,
    projetoid UUID NOT NULL REFERENCES athena.projeto(id) ON DELETE RESTRICT,
    titulo VARCHAR(240) NOT NULL,
    grupo VARCHAR(120) NOT NULL,
    categoria VARCHAR(120) NOT NULL,
    finalidade VARCHAR(240) NOT NULL,
    apresentacao VARCHAR(240) NOT NULL,
    criadoem TIMESTAMPTZ NOT NULL,
    criadoporid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    UNIQUE (projetoid, identificador)
);

CREATE TABLE athena.versaodocumento (
    id UUID PRIMARY KEY,
    documentoid UUID NOT NULL REFERENCES athena.documento(id) ON DELETE RESTRICT,
    numero INTEGER NOT NULL CHECK (numero > 0),
    formato VARCHAR(40) NOT NULL,
    conteudooriginal TEXT NOT NULL,
    hashconteudo VARCHAR(128) NOT NULL,
    recebidaem TIMESTAMPTZ NOT NULL,
    recebidaporid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    origemexterna VARCHAR(240),
    UNIQUE (documentoid, numero)
);

CREATE TABLE athena.itemdocumental (
    id UUID PRIMARY KEY,
    versaoid UUID NOT NULL REFERENCES athena.versaodocumento(id) ON DELETE RESTRICT,
    identificador VARCHAR(160) NOT NULL,
    titulo VARCHAR(240) NOT NULL,
    conteudo TEXT NOT NULL,
    ordem INTEGER NOT NULL CHECK (ordem > 0),
    fontesrastreabilidade TEXT[] NOT NULL,
    UNIQUE (versaoid, identificador)
);

CREATE TABLE athena.avaliacao (
    id UUID PRIMARY KEY,
    itemid UUID NOT NULL REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    responsavelid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    decisao VARCHAR(80) NOT NULL,
    justificativa TEXT NOT NULL,
    avaliadaem TIMESTAMPTZ NOT NULL
);

CREATE TABLE athena.comentario (
    id UUID PRIMARY KEY,
    itemid UUID NOT NULL REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    autorid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    conteudo TEXT NOT NULL,
    registradoem TIMESTAMPTZ NOT NULL,
    origem VARCHAR(120) NOT NULL
);

CREATE TABLE athena.pedidoajuste (
    id UUID PRIMARY KEY,
    itemid UUID NOT NULL REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    solicitanteid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    descricao TEXT NOT NULL,
    justificativa TEXT NOT NULL,
    situacao VARCHAR(60) NOT NULL,
    solicitadoem TIMESTAMPTZ NOT NULL,
    concluidoem TIMESTAMPTZ
);

CREATE TABLE athena.confirmacao (
    id UUID PRIMARY KEY,
    itemid UUID NOT NULL REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    responsavelid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    decisao VARCHAR(80) NOT NULL,
    justificativa TEXT NOT NULL,
    confirmadaem TIMESTAMPTZ NOT NULL
);

CREATE TABLE athena.relacaorecebida (
    id UUID PRIMARY KEY,
    documentoid UUID REFERENCES athena.documento(id) ON DELETE RESTRICT,
    itemid UUID REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    tipo VARCHAR(80) NOT NULL,
    recursorelacionado VARCHAR(240) NOT NULL,
    recebidaem TIMESTAMPTZ NOT NULL,
    recebidaporid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    CHECK (documentoid IS NOT NULL OR itemid IS NOT NULL)
);

CREATE TABLE athena.eventoauditoria (
    id UUID PRIMARY KEY,
    projetoid UUID REFERENCES athena.projeto(id) ON DELETE RESTRICT,
    documentoid UUID REFERENCES athena.documento(id) ON DELETE RESTRICT,
    versaoid UUID REFERENCES athena.versaodocumento(id) ON DELETE RESTRICT,
    itemid UUID REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    usuarioid UUID REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    tipo VARCHAR(100) NOT NULL,
    decisao VARCHAR(100),
    justificativa TEXT,
    dados JSONB NOT NULL DEFAULT '{}'::jsonb,
    ocorridoem TIMESTAMPTZ NOT NULL,
    registradoporid UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT
);

CREATE INDEX idxvinculoprojetoprojeto ON athena.vinculoprojeto (projetoid);
CREATE INDEX idxdocumentoprojeto ON athena.documento (projetoid);
CREATE INDEX idxversaodocumentodocumento ON athena.versaodocumento (documentoid, numero);
CREATE INDEX idxitemdocumentalversao ON athena.itemdocumental (versaoid);
CREATE INDEX idxavaliacaoitem ON athena.avaliacao (itemid);
CREATE INDEX idxcomentarioitem ON athena.comentario (itemid);
CREATE INDEX idxpedidoajusteitem ON athena.pedidoajuste (itemid);
CREATE INDEX idxconfirmacaoitem ON athena.confirmacao (itemid);
CREATE INDEX idxrelacaorecebidaitem ON athena.relacaorecebida (itemid);
CREATE INDEX idxeventoauditoriaprojeto ON athena.eventoauditoria (projetoid, ocorridoem);
CREATE INDEX idxeventoauditoriadocumento ON athena.eventoauditoria (documentoid, ocorridoem);

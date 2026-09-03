CREATE SCHEMA IF NOT EXISTS athena;

CREATE TABLE athena.usuarioinstitucional (
    id UUID PRIMARY KEY,
    identificador VARCHAR(120) NOT NULL UNIQUE,
    nome VARCHAR(200) NOT NULL,
    login VARCHAR(120) NOT NULL UNIQUE,
    unidade VARCHAR(80) NOT NULL,
    datacadastro TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE athena.projeto (
    id UUID PRIMARY KEY,
    identificador VARCHAR(80) NOT NULL UNIQUE,
    nome VARCHAR(200) NOT NULL,
    descricao TEXT NOT NULL,
    situacao VARCHAR(40) NOT NULL,
    datacriacao TIMESTAMPTZ NOT NULL,
    id_usuariocriacao UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    dataalteracao TIMESTAMPTZ,
    id_usuarioalteracao UUID REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT
);

CREATE TABLE athena.vinculoprojeto (
    id UUID PRIMARY KEY,
    id_projeto UUID NOT NULL REFERENCES athena.projeto(id) ON DELETE RESTRICT,
    id_usuario UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    papel VARCHAR(60) NOT NULL,
    unidade VARCHAR(80) NOT NULL,
    datainicio TIMESTAMPTZ NOT NULL,
    datafim TIMESTAMPTZ,
    id_usuarioregistro UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    UNIQUE (id_projeto, id_usuario, papel, datainicio)
);

CREATE TABLE athena.documento (
    id UUID PRIMARY KEY,
    identificador VARCHAR(120) NOT NULL,
    id_projeto UUID NOT NULL REFERENCES athena.projeto(id) ON DELETE RESTRICT,
    titulo VARCHAR(240) NOT NULL,
    grupo VARCHAR(120) NOT NULL,
    categoria VARCHAR(120) NOT NULL,
    finalidade VARCHAR(240) NOT NULL,
    apresentacao VARCHAR(240) NOT NULL,
    datacriacao TIMESTAMPTZ NOT NULL,
    id_usuariocriacao UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    UNIQUE (id_projeto, identificador)
);

CREATE TABLE athena.versaodocumento (
    id UUID PRIMARY KEY,
    id_documento UUID NOT NULL REFERENCES athena.documento(id) ON DELETE RESTRICT,
    numero INTEGER NOT NULL CHECK (numero > 0),
    formato VARCHAR(40) NOT NULL,
    conteudooriginal TEXT NOT NULL,
    hashconteudo VARCHAR(128) NOT NULL,
    datarecebimento TIMESTAMPTZ NOT NULL,
    id_usuariorecebimento UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    origemexterna VARCHAR(240),
    UNIQUE (id_documento, numero)
);

CREATE TABLE athena.itemdocumental (
    id UUID PRIMARY KEY,
    id_versao UUID NOT NULL REFERENCES athena.versaodocumento(id) ON DELETE RESTRICT,
    identificador VARCHAR(160) NOT NULL,
    titulo VARCHAR(240) NOT NULL,
    conteudo TEXT NOT NULL,
    ordem INTEGER NOT NULL CHECK (ordem > 0),
    fontesrastreabilidade TEXT[] NOT NULL,
    UNIQUE (id_versao, identificador)
);

CREATE TABLE athena.avaliacao (
    id UUID PRIMARY KEY,
    id_item UUID NOT NULL REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    id_responsavel UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    decisao VARCHAR(80) NOT NULL,
    justificativa TEXT NOT NULL,
    dataavaliacao TIMESTAMPTZ NOT NULL
);

CREATE TABLE athena.comentario (
    id UUID PRIMARY KEY,
    id_item UUID NOT NULL REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    id_autor UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    conteudo TEXT NOT NULL,
    dataregistro TIMESTAMPTZ NOT NULL,
    origem VARCHAR(120) NOT NULL
);

CREATE TABLE athena.pedidoajuste (
    id UUID PRIMARY KEY,
    id_item UUID NOT NULL REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    id_solicitante UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    descricao TEXT NOT NULL,
    justificativa TEXT NOT NULL,
    situacao VARCHAR(60) NOT NULL,
    datasolicitacao TIMESTAMPTZ NOT NULL,
    dataconclusao TIMESTAMPTZ
);

CREATE TABLE athena.confirmacao (
    id UUID PRIMARY KEY,
    id_item UUID NOT NULL REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    id_responsavel UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    decisao VARCHAR(80) NOT NULL,
    justificativa TEXT NOT NULL,
    dataconfirmacao TIMESTAMPTZ NOT NULL
);

CREATE TABLE athena.relacaorecebida (
    id UUID PRIMARY KEY,
    id_documento UUID REFERENCES athena.documento(id) ON DELETE RESTRICT,
    id_item UUID REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    tipo VARCHAR(80) NOT NULL,
    recursorelacionado VARCHAR(240) NOT NULL,
    datarecebimento TIMESTAMPTZ NOT NULL,
    id_usuariorecebimento UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    CHECK (id_documento IS NOT NULL OR id_item IS NOT NULL)
);

CREATE TABLE athena.eventoauditoria (
    id UUID PRIMARY KEY,
    id_projeto UUID REFERENCES athena.projeto(id) ON DELETE RESTRICT,
    id_documento UUID REFERENCES athena.documento(id) ON DELETE RESTRICT,
    id_versao UUID REFERENCES athena.versaodocumento(id) ON DELETE RESTRICT,
    id_item UUID REFERENCES athena.itemdocumental(id) ON DELETE RESTRICT,
    id_usuario UUID REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT,
    tipo VARCHAR(100) NOT NULL,
    decisao VARCHAR(100),
    justificativa TEXT,
    dados JSONB NOT NULL DEFAULT '{}'::jsonb,
    dataocorrencia TIMESTAMPTZ NOT NULL,
    id_usuarioregistro UUID NOT NULL REFERENCES athena.usuarioinstitucional(id) ON DELETE RESTRICT
);

CREATE INDEX idx_vinculoprojeto_projeto ON athena.vinculoprojeto (id_projeto);
CREATE INDEX idx_documento_projeto ON athena.documento (id_projeto);
CREATE INDEX idx_versaodocumento_documento ON athena.versaodocumento (id_documento, numero);
CREATE INDEX idx_itemdocumental_versao ON athena.itemdocumental (id_versao);
CREATE INDEX idx_avaliacao_item ON athena.avaliacao (id_item);
CREATE INDEX idx_comentario_item ON athena.comentario (id_item);
CREATE INDEX idx_pedidoajuste_item ON athena.pedidoajuste (id_item);
CREATE INDEX idx_confirmacao_item ON athena.confirmacao (id_item);
CREATE INDEX idx_relacaorecebida_item ON athena.relacaorecebida (id_item);
CREATE INDEX idx_eventoauditoria_projeto ON athena.eventoauditoria (id_projeto, dataocorrencia);
CREATE INDEX idx_eventoauditoria_documento ON athena.eventoauditoria (id_documento, dataocorrencia);

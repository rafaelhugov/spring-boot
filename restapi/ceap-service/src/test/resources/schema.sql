CREATE SCHEMA IF NOT EXISTS recursoshumanos;
CREATE SCHEMA IF NOT EXISTS tj;

CREATE ALIAS IF NOT EXISTS funcaosemacentos FOR "br.gov.mppb.ncap.infrastructure.config.H2Functions.funcaoSemAcentos";

CREATE TABLE IF NOT EXISTS public.municipio (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    excluido BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS tj.comarca (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    id_municipio BIGINT,
    excluido BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS tj.termo (
    id_municipio BIGINT,
    id_comarca BIGINT,
    PRIMARY KEY (id_municipio, id_comarca)
);

CREATE TABLE IF NOT EXISTS public.pessoa (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS recursoshumanos.tipocargo (
    id BIGINT PRIMARY KEY,
    tipo CHAR(1)
);

CREATE TABLE IF NOT EXISTS recursoshumanos.funcao (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    id_tipocargo BIGINT,
    FOREIGN KEY (id_tipocargo) REFERENCES recursoshumanos.tipocargo(id)
);

CREATE TABLE IF NOT EXISTS public.servidor (
    id BIGINT PRIMARY KEY,
    id_pessoa BIGINT,
    id_cargo BIGINT,
    matricula VARCHAR(50),
    ativo CHAR(1),
    excluido BOOLEAN,
    FOREIGN KEY (id_pessoa) REFERENCES public.pessoa(id),
    FOREIGN KEY (id_cargo) REFERENCES recursoshumanos.funcao(id)
);

CREATE TABLE IF NOT EXISTS ncap.auditoria (
    id_auditoria BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_inspecao BIGINT NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    acao TEXT NOT NULL,
    datahora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ncap.formtemplates (
    id_formtemplate BIGINT AUTO_INCREMENT PRIMARY KEY,
    resolucao VARCHAR(50) NOT NULL,
    tipoentidade VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criadoem TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ncap.secoes (
    id_secao BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_template BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    ordem INTEGER NOT NULL,
    FOREIGN KEY (id_template) REFERENCES ncap.formtemplates (id_formtemplate) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ncap.blocos (
    id_bloco BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_secao BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    ordem INTEGER NOT NULL,
    FOREIGN KEY (id_secao) REFERENCES ncap.secoes (id_secao) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ncap.perguntas (
    id_pergunta BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_bloco BIGINT NOT NULL,
    idcnmp VARCHAR(100),
    label TEXT NOT NULL,
    campotipo VARCHAR(50) NOT NULL,
    obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
    exportarcnmp BOOLEAN NOT NULL DEFAULT TRUE,
    ordem INTEGER NOT NULL,
    tabulacao INTEGER NOT NULL DEFAULT 0,
    id_dependenciapai BIGINT,
    dependenciavalor VARCHAR(255),
    FOREIGN KEY (id_bloco) REFERENCES ncap.blocos (id_bloco) ON DELETE CASCADE,
    FOREIGN KEY (id_dependenciapai) REFERENCES ncap.perguntas (id_pergunta) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS ncap.opcoesresposta (
    id_opcaoresposta BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_pergunta BIGINT NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    valor VARCHAR(255) NOT NULL,
    FOREIGN KEY (id_pergunta) REFERENCES ncap.perguntas (id_pergunta) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ncap.respostasinspecao (
    id_respostainspecao BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_inspecao BIGINT NOT NULL,
    id_pergunta BIGINT NOT NULL,
    valorresposta TEXT,
    observacaoprevisita TEXT,
    observacaocorregedoria TEXT,
    FOREIGN KEY (id_pergunta) REFERENCES ncap.perguntas (id_pergunta) ON DELETE CASCADE,
    UNIQUE (id_inspecao, id_pergunta)
);

-- =============================================================================
-- SQL SCHEMA: NCAP - SISTEMA DE INSPEÇÃO (POSTGRESQL COMPATIBLE)
-- Padrões de Nomenclatura:
--   - Tabelas: Aglutinadas (ex: formtemplates)
--   - Chaves Primárias: id_campo (ex: id_formtemplate)
--   - Chaves Estrangeiras / Referências: id_campo (ex: id_template, id_secao, id_dependenciapai)
--   - Campos Compostos (Não FK): Aglutinados (ex: campotipo, exportarcnmp)
--   - Constraints FK: fk_[tabela][referenciada] (ex: fk_secoestemplates)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ncap;
SET search_path TO ncap, public;

-- 1. Tabela de Templates de Formulário
CREATE TABLE IF NOT EXISTS ncap.formtemplates (
    id_formtemplate BIGSERIAL PRIMARY KEY,
    resolucao VARCHAR(50) NOT NULL,
    tipoentidade VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criadoem TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Seções do Formulário
CREATE TABLE IF NOT EXISTS ncap.secoes (
    id_secao BIGSERIAL PRIMARY KEY,
    id_template BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    ordem INTEGER NOT NULL,
    CONSTRAINT fk_secoestemplates FOREIGN KEY (id_template) REFERENCES ncap.formtemplates (id_formtemplate) ON DELETE CASCADE
);

-- 3. Tabela de Blocos da Seção
CREATE TABLE IF NOT EXISTS ncap.blocos (
    id_bloco BIGSERIAL PRIMARY KEY,
    id_secao BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    ordem INTEGER NOT NULL,
    CONSTRAINT fk_blocossecao FOREIGN KEY (id_secao) REFERENCES ncap.secoes (id_secao) ON DELETE CASCADE
);

-- 4. Tabela de Perguntas
CREATE TABLE IF NOT EXISTS ncap.perguntas (
    id_pergunta BIGSERIAL PRIMARY KEY,
    id_bloco BIGINT NOT NULL,
    idcnmp VARCHAR(100), -- Código original do CNMP (null para campos locais)
    label TEXT NOT NULL,
    campotipo VARCHAR(50) NOT NULL, -- TEXTO, SIM_NAO, RADIO, CHECKBOX, SELECAO_MULTIPLA, COMBO_BOX, LABEL, etc.
    obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
    exportarcnmp BOOLEAN NOT NULL DEFAULT TRUE, -- FALSE para anotações/campos locais do fluxo de trabalho
    ordem INTEGER NOT NULL,
    tabulacao INTEGER NOT NULL DEFAULT 0, -- recuo visual
    id_dependenciapai BIGINT, -- ID de pergunta da qual depende
    dependenciavalor VARCHAR(255), -- valor que exibe a pergunta
    CONSTRAINT fk_perguntasbloco FOREIGN KEY (id_bloco) REFERENCES ncap.blocos (id_bloco) ON DELETE CASCADE,
    CONSTRAINT fk_perguntasdependencia FOREIGN KEY (id_dependenciapai) REFERENCES ncap.perguntas (id_pergunta) ON DELETE SET NULL
);

-- 5. Opções de Resposta para Escolhas
CREATE TABLE IF NOT EXISTS ncap.opcoesresposta (
    id_opcaoresposta BIGSERIAL PRIMARY KEY,
    id_pergunta BIGINT NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    valor VARCHAR(255) NOT NULL,
    CONSTRAINT fk_opcoespergunta FOREIGN KEY (id_pergunta) REFERENCES ncap.perguntas (id_pergunta) ON DELETE CASCADE
);

-- 6. Tabela de Entidades a serem Inspecionadas
CREATE TABLE IF NOT EXISTS ncap.entidades (
    id_entidade BIGSERIAL PRIMARY KEY,
    id_cnmp BIGINT UNIQUE, -- ID oficial do CNMP para sincronização
    nomecnmp VARCHAR(255) NOT NULL,
    descricaomppb VARCHAR(255),
    tipoentidade VARCHAR(150) NOT NULL,
    id_comarca BIGINT NOT NULL,
    municipio VARCHAR(255) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criadoem TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Tabela Core de Inspeções
CREATE TABLE IF NOT EXISTS ncap.inspecoes (
    id_inspecao BIGSERIAL PRIMARY KEY,
    id_entidade BIGINT NOT NULL,
    id_promotorresponsavel BIGINT NOT NULL,
    id_servidor_criador BIGINT NOT NULL,
    id_formtemplate BIGINT,
    status VARCHAR(50) NOT NULL, -- AGENDADA, OBSERVACAO_MEMBRO, EM_PREENCHIMENTO, VALIDACAO_CORREGEDORIA, CONCLUIDA
    dataagendamento DATE NOT NULL,
    datavisita DATE,
    semestre VARCHAR(20) NOT NULL, -- ex: "2026.1"
    numero_pga VARCHAR(50),
    link_pga VARCHAR(500),
    tipo_visita VARCHAR(20),
    data_limite_resposta DATE,
    justificativa_remota VARCHAR(60),
    justificativa_detalhe VARCHAR(2000),
    responsavel_principal VARCHAR(200),
    email_responsavel VARCHAR(200),
    tokenacesso VARCHAR(255) UNIQUE, -- Token seguro de acesso externo para a delegacia preencher autoavaliação
    tokenexpiracao TIMESTAMP WITH TIME ZONE, -- Prazo de expiração do token de segurança
    criadoem TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizadoem TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inspecoesentidade FOREIGN KEY (id_entidade) REFERENCES ncap.entidades (id_entidade) ON DELETE RESTRICT,
    CONSTRAINT fk_inspecoes_formtemplate FOREIGN KEY (id_formtemplate) REFERENCES ncap.formtemplates (id_formtemplate) ON DELETE RESTRICT
);

-- Tabela Associativa para Equipe Inspetora (Membros)
CREATE TABLE IF NOT EXISTS ncap.inspecao_membros (
    id_inspecao BIGINT NOT NULL,
    id_servidor BIGINT NOT NULL,
    PRIMARY KEY (id_inspecao, id_servidor),
    CONSTRAINT fk_membrosinspecao FOREIGN KEY (id_inspecao) REFERENCES ncap.inspecoes (id_inspecao) ON DELETE CASCADE
);

-- 8. Tabela de Respostas das Perguntas por Inspeção
CREATE TABLE IF NOT EXISTS ncap.respostasinspecao (
    id_respostainspecao BIGSERIAL PRIMARY KEY,
    id_inspecao BIGINT NOT NULL,
    id_pergunta BIGINT NOT NULL,
    valorresposta TEXT,
    observacaoprevisita TEXT, -- observação local do promotor
    observacaocorregedoria TEXT, -- observação local da corregedoria
    CONSTRAINT fk_respostasinspecao FOREIGN KEY (id_inspecao) REFERENCES ncap.inspecoes (id_inspecao) ON DELETE CASCADE,
    CONSTRAINT fk_respostaspegunta FOREIGN KEY (id_pergunta) REFERENCES ncap.perguntas (id_pergunta) ON DELETE CASCADE,
    CONSTRAINT uq_inspecaopergunta UNIQUE (id_inspecao, id_pergunta)
);

-- 9. Tabela de Logs de Auditoria
CREATE TABLE IF NOT EXISTS ncap.auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,
    id_inspecao BIGINT NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    acao TEXT NOT NULL,
    datahora TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auditoriainspecao FOREIGN KEY (id_inspecao) REFERENCES ncap.inspecoes (id_inspecao) ON DELETE CASCADE
);

-- Índices de Otimização de Performance
CREATE INDEX IF NOT EXISTS idx_entidadescomarca ON ncap.entidades(id_comarca);
CREATE UNIQUE INDEX IF NOT EXISTS idx_entidades_idcnmp ON ncap.entidades(id_cnmp);
CREATE INDEX IF NOT EXISTS idx_inspecoesstatus ON ncap.inspecoes(status);
CREATE INDEX IF NOT EXISTS idx_inspecoesentidade ON ncap.inspecoes(id_entidade);
CREATE INDEX IF NOT EXISTS idx_respostasinspecao ON ncap.respostasinspecao(id_inspecao);
CREATE INDEX IF NOT EXISTS idx_perguntasidcnmp ON ncap.perguntas(idcnmp);

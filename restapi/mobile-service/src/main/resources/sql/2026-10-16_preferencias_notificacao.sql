-- Executar nos bancos MPE (producao) e ATENDIMENTO (homologacao).
-- Usuario da aplicacao nos dois ambientes: android.

CREATE TABLE IF NOT EXISTS diariooficial.preferencianotificacao (
    id BIGSERIAL PRIMARY KEY,
    servidormatricula VARCHAR(20) NOT NULL,
    noticias BOOLEAN NOT NULL DEFAULT TRUE,
    designacoes BOOLEAN NOT NULL DEFAULT TRUE,
    contracheques BOOLEAN NOT NULL DEFAULT TRUE,
    pagamentos BOOLEAN NOT NULL DEFAULT TRUE,
    afastamentos BOOLEAN NOT NULL DEFAULT TRUE,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_preferencianotificacao_matricula
        UNIQUE (servidormatricula)
);

GRANT USAGE ON SCHEMA diariooficial TO android;

GRANT SELECT, INSERT, UPDATE ON TABLE
    diariooficial.preferencianotificacao
TO android;

GRANT USAGE, SELECT ON SEQUENCE
    diariooficial.preferencianotificacao_id_seq
TO android;

CREATE TABLE IF NOT EXISTS diariooficial.dispositivofcm (
    id BIGSERIAL PRIMARY KEY,
    identificador VARCHAR(64) NOT NULL,
    servidormatricula VARCHAR(20) NOT NULL,
    tokenfcm TEXT NOT NULL,
    tokenfcmhash CHAR(64) NOT NULL,
    plataforma VARCHAR(10) NOT NULL
        CHECK (plataforma IN ('ANDROID', 'IOS')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    datacriacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataultimaatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataultimouso TIMESTAMP,

    CONSTRAINT uk_dispositivofcm_identificador
        UNIQUE (identificador),

    CONSTRAINT uk_dispositivofcm_tokenfcmhash
        UNIQUE (tokenfcmhash)
);

CREATE INDEX IF NOT EXISTS idx_dispositivofcm_matricula_ativo
    ON diariooficial.dispositivofcm (
        servidormatricula,
        ativo
    );

CREATE INDEX IF NOT EXISTS idx_dispositivofcm_plataforma
    ON diariooficial.dispositivofcm (
        plataforma
    );

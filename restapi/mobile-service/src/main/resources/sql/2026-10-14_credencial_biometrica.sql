CREATE TABLE IF NOT EXISTS diariooficial.credencialbiometrica (
    id BIGSERIAL PRIMARY KEY,
    seletor UUID NOT NULL,
    segredohash CHAR(64) NOT NULL,
    login VARCHAR(150) NOT NULL,
    dispositivoid VARCHAR(64) NOT NULL,
    plataforma VARCHAR(10) NOT NULL CHECK (plataforma IN ('ANDROID', 'IOS')),
    tokeninstitucionalcifrado TEXT NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    expiraem TIMESTAMP NOT NULL,
    datacriacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataultimaatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataultimouso TIMESTAMP,

    CONSTRAINT uk_credencialbiometrica_seletor UNIQUE (seletor),
    CONSTRAINT uk_credencialbiometrica_login_dispositivo UNIQUE (login, dispositivoid)
);

CREATE INDEX IF NOT EXISTS idx_credencialbiometrica_login_ativa
    ON diariooficial.credencialbiometrica (login, ativa);

CREATE INDEX IF NOT EXISTS idx_credencialbiometrica_expiracao
    ON diariooficial.credencialbiometrica (expiraem)
    WHERE ativa = TRUE;

GRANT SELECT, INSERT, UPDATE ON diariooficial.credencialbiometrica TO android;
GRANT USAGE, SELECT ON SEQUENCE diariooficial.credencialbiometrica_id_seq TO android;

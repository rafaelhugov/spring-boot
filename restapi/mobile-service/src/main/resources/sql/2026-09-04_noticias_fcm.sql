CREATE TABLE IF NOT EXISTS diariooficial.noticiaconhecida (
    id BIGSERIAL PRIMARY KEY,
    chaveexterna VARCHAR(128) NOT NULL,
    link TEXT,
    titulohash CHAR(64),
    datapublicacao TIMESTAMP,
    origemregistro CHAR(1) NOT NULL DEFAULT 'N'
        CHECK (origemregistro IN ('I', 'N')),
    statusnotificacao VARCHAR(2) NOT NULL DEFAULT 'PE'
        CHECK (statusnotificacao IN ('PE', 'EN', 'ER', 'NA')),
    quantidadetentativas INTEGER NOT NULL DEFAULT 0
        CHECK (quantidadetentativas >= 0),
    ultimamensagemerro VARCHAR(500),
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    datanotificacao TIMESTAMP,
    datareservaprocessamento TIMESTAMP,
    identificadorreserva VARCHAR(64),

    CONSTRAINT uk_noticiaconhecida_chaveexterna
        UNIQUE (chaveexterna)
);

CREATE INDEX IF NOT EXISTS idx_noticiaconhecida_status_tentativas
    ON diariooficial.noticiaconhecida (
        origemregistro,
        statusnotificacao,
        quantidadetentativas,
        dataregistro,
        id
    );

CREATE INDEX IF NOT EXISTS idx_noticiaconhecida_reserva
    ON diariooficial.noticiaconhecida (
        datareservaprocessamento,
        identificadorreserva
    );

CREATE INDEX IF NOT EXISTS idx_noticiaconhecida_datapublicacao
    ON diariooficial.noticiaconhecida (
        datapublicacao
    );

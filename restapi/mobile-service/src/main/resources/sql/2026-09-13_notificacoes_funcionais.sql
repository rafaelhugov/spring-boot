CREATE TABLE IF NOT EXISTS diariooficial.contrachequenotificacaocontrole (
    id BIGSERIAL PRIMARY KEY,
    servidormatricula VARCHAR(20) NOT NULL,
    inicializado BOOLEAN NOT NULL DEFAULT FALSE,
    datainicializacao TIMESTAMP,
    dataultimoprocessamento TIMESTAMP,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_contrachequenotificacaocontrole
        UNIQUE (servidormatricula)
);

CREATE TABLE IF NOT EXISTS diariooficial.contrachequenotificacaoconhecida (
    id BIGSERIAL PRIMARY KEY,
    servidormatricula VARCHAR(20) NOT NULL,
    chaveevento VARCHAR(160) NOT NULL,
    tipoevento VARCHAR(64) NOT NULL,
    rota VARCHAR(128) NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    corpo VARCHAR(220) NOT NULL,
    origemregistro CHAR(1) NOT NULL
        CHECK (origemregistro IN ('I', 'N')),
    statusnotificacao CHAR(2) NOT NULL
        CHECK (statusnotificacao IN ('NA', 'PE', 'EN', 'ER')),
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    datanotificacao TIMESTAMP,
    datareservaprocessamento TIMESTAMP,
    identificadorreserva VARCHAR(64),
    quantidadetentativas INTEGER NOT NULL DEFAULT 0
        CHECK (quantidadetentativas >= 0),
    ultimamensagemerro VARCHAR(500),

    CONSTRAINT uk_contrachequenotificacaoconhecida
        UNIQUE (servidormatricula, chaveevento),

    CONSTRAINT ck_contrachequenotificacao_origem_status
        CHECK (
            (origemregistro = 'I' AND statusnotificacao = 'NA')
            OR
            (origemregistro = 'N' AND statusnotificacao IN ('PE', 'EN', 'ER'))
        )
);

CREATE TABLE IF NOT EXISTS diariooficial.pagamentonotificacaocontrole (
    id BIGSERIAL PRIMARY KEY,
    servidormatricula VARCHAR(20) NOT NULL,
    inicializado BOOLEAN NOT NULL DEFAULT FALSE,
    datainicializacao TIMESTAMP,
    dataultimoprocessamento TIMESTAMP,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_pagamentonotificacaocontrole
        UNIQUE (servidormatricula)
);

CREATE TABLE IF NOT EXISTS diariooficial.pagamentonotificacaoconhecida (
    id BIGSERIAL PRIMARY KEY,
    servidormatricula VARCHAR(20) NOT NULL,
    chaveevento VARCHAR(160) NOT NULL,
    tipoevento VARCHAR(64) NOT NULL,
    rota VARCHAR(128) NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    corpo VARCHAR(220) NOT NULL,
    origemregistro CHAR(1) NOT NULL
        CHECK (origemregistro IN ('I', 'N')),
    statusnotificacao CHAR(2) NOT NULL
        CHECK (statusnotificacao IN ('NA', 'PE', 'EN', 'ER')),
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    datanotificacao TIMESTAMP,
    datareservaprocessamento TIMESTAMP,
    identificadorreserva VARCHAR(64),
    quantidadetentativas INTEGER NOT NULL DEFAULT 0
        CHECK (quantidadetentativas >= 0),
    ultimamensagemerro VARCHAR(500),

    CONSTRAINT uk_pagamentonotificacaoconhecida
        UNIQUE (servidormatricula, chaveevento),

    CONSTRAINT ck_pagamentonotificacao_origem_status
        CHECK (
            (origemregistro = 'I' AND statusnotificacao = 'NA')
            OR
            (origemregistro = 'N' AND statusnotificacao IN ('PE', 'EN', 'ER'))
        )
);

CREATE TABLE IF NOT EXISTS diariooficial.afastamentonotificacaocontrole (
    id BIGSERIAL PRIMARY KEY,
    servidormatricula VARCHAR(20) NOT NULL,
    inicializado BOOLEAN NOT NULL DEFAULT FALSE,
    datainicializacao TIMESTAMP,
    dataultimoprocessamento TIMESTAMP,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_afastamentonotificacaocontrole
        UNIQUE (servidormatricula)
);

CREATE TABLE IF NOT EXISTS diariooficial.afastamentonotificacaoconhecida (
    id BIGSERIAL PRIMARY KEY,
    servidormatricula VARCHAR(20) NOT NULL,
    chaveevento VARCHAR(160) NOT NULL,
    tipoevento VARCHAR(64) NOT NULL,
    rota VARCHAR(128) NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    corpo VARCHAR(220) NOT NULL,
    origemregistro CHAR(1) NOT NULL
        CHECK (origemregistro IN ('I', 'N')),
    statusnotificacao CHAR(2) NOT NULL
        CHECK (statusnotificacao IN ('NA', 'PE', 'EN', 'ER')),
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    datanotificacao TIMESTAMP,
    datareservaprocessamento TIMESTAMP,
    identificadorreserva VARCHAR(64),
    quantidadetentativas INTEGER NOT NULL DEFAULT 0
        CHECK (quantidadetentativas >= 0),
    ultimamensagemerro VARCHAR(500),

    CONSTRAINT uk_afastamentonotificacaoconhecida
        UNIQUE (servidormatricula, chaveevento),

    CONSTRAINT ck_afastamentonotificacao_origem_status
        CHECK (
            (origemregistro = 'I' AND statusnotificacao = 'NA')
            OR
            (origemregistro = 'N' AND statusnotificacao IN ('PE', 'EN', 'ER'))
        )
);

CREATE INDEX IF NOT EXISTS idx_contrachequenotificacaocontrole_inicializado
    ON diariooficial.contrachequenotificacaocontrole (inicializado, servidormatricula);

CREATE INDEX IF NOT EXISTS idx_contrachequenotificacaoconhecida_status
    ON diariooficial.contrachequenotificacaoconhecida (
        origemregistro,
        statusnotificacao,
        quantidadetentativas,
        dataregistro,
        id
    );

CREATE INDEX IF NOT EXISTS idx_contrachequenotificacaoconhecida_reserva
    ON diariooficial.contrachequenotificacaoconhecida (
        datareservaprocessamento,
        identificadorreserva
    );

CREATE INDEX IF NOT EXISTS idx_pagamentonotificacaocontrole_inicializado
    ON diariooficial.pagamentonotificacaocontrole (inicializado, servidormatricula);

CREATE INDEX IF NOT EXISTS idx_pagamentonotificacaoconhecida_status
    ON diariooficial.pagamentonotificacaoconhecida (
        origemregistro,
        statusnotificacao,
        quantidadetentativas,
        dataregistro,
        id
    );

CREATE INDEX IF NOT EXISTS idx_pagamentonotificacaoconhecida_reserva
    ON diariooficial.pagamentonotificacaoconhecida (
        datareservaprocessamento,
        identificadorreserva
    );

CREATE INDEX IF NOT EXISTS idx_afastamentonotificacaocontrole_inicializado
    ON diariooficial.afastamentonotificacaocontrole (inicializado, servidormatricula);

CREATE INDEX IF NOT EXISTS idx_afastamentonotificacaoconhecida_status
    ON diariooficial.afastamentonotificacaoconhecida (
        origemregistro,
        statusnotificacao,
        quantidadetentativas,
        dataregistro,
        id
    );

CREATE INDEX IF NOT EXISTS idx_afastamentonotificacaoconhecida_reserva
    ON diariooficial.afastamentonotificacaoconhecida (
        datareservaprocessamento,
        identificadorreserva
    );

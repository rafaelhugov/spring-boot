CREATE TABLE IF NOT EXISTS diariooficial.designacaousuariocontrole (
    id BIGSERIAL PRIMARY KEY,
    servidor_matricula VARCHAR(20) NOT NULL,
    inicializado BOOLEAN NOT NULL DEFAULT FALSE,
    datainicializacao TIMESTAMP,
    dataultimoprocessamento TIMESTAMP,
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataatualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_designacaousuariocontrole_matricula
        UNIQUE (servidor_matricula)
);

CREATE TABLE IF NOT EXISTS diariooficial.designacaoconhecida (
    id BIGSERIAL PRIMARY KEY,
    servidormatricula VARCHAR(20) NOT NULL,
    id_designacao INTEGER NOT NULL
        REFERENCES diariooficial.designacao,
    tipodesignacao VARCHAR(2) NOT NULL,
    datapublicacao DATE,
    origemregistro CHAR(1) NOT NULL
        CHECK (origemregistro IN ('I', 'N')),
    statusnotificacao CHAR(2) NOT NULL
        CHECK (statusnotificacao IN ('NA', 'PE', 'EN', 'ER')),
    dataregistro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    datanotificacao TIMESTAMP,
    quantidadetentativas INTEGER NOT NULL DEFAULT 0,
    ultimamensagemerro VARCHAR(500),

    CONSTRAINT uk_designacaoconhecida
        UNIQUE (
            servidormatricula,
            id_designacao,
            tipodesignacao
        ),

    CONSTRAINT ck_designacaoconhecida_origem_status
        CHECK (
            (origemregistro = 'I' AND statusnotificacao = 'NA')
            OR
            (origemregistro = 'N' AND statusnotificacao IN ('PE', 'EN', 'ER'))
        )
);

CREATE INDEX IF NOT EXISTS idx_designacaousuariocontrole_inicializado
    ON diariooficial.designacaousuariocontrole (
        inicializado,
        servidor_matricula
    );

CREATE INDEX IF NOT EXISTS idx_designacaoconhecida_matricula
    ON diariooficial.designacaoconhecida (
        servidormatricula
    );

CREATE INDEX IF NOT EXISTS idx_designacaoconhecida_status
    ON diariooficial.designacaoconhecida (
        statusnotificacao,
        quantidadetentativas
    );

CREATE INDEX IF NOT EXISTS idx_designacaoconhecida_publicacao
    ON diariooficial.designacaoconhecida (
        datapublicacao
    );

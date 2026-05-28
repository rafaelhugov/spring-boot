CREATE TABLE IF NOT EXISTS cursos.frequenciaotptoken (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(6) NOT NULL,
    datacriacao TIMESTAMP NOT NULL,
    dataexpiracao TIMESTAMP NOT NULL,
    id_chamada BIGINT NOT NULL,
    id_inscricao BIGINT NOT NULL,
    CONSTRAINT fk_frequenciaotptoken_chamada FOREIGN KEY (id_chamada) REFERENCES cursos.chamada (id),
    CONSTRAINT fk_frequenciaotptoken_inscricao FOREIGN KEY (id_inscricao) REFERENCES cursos.inscricao (id)
);

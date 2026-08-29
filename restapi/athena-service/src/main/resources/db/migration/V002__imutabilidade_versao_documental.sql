ALTER TABLE athena.versaodocumento
    ADD COLUMN versaoanteriorid UUID REFERENCES athena.versaodocumento(id) ON DELETE RESTRICT,
    ADD COLUMN confirmada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pedidoajusteorigemid UUID;

ALTER TABLE athena.versaodocumento
    ADD CONSTRAINT ckversaodocumentovinculoanterior
    CHECK ((numero = 1 AND versaoanteriorid IS NULL) OR (numero > 1 AND versaoanteriorid IS NOT NULL));

ALTER TABLE athena.pedidoajuste
    ADD COLUMN versaoresultanteid UUID REFERENCES athena.versaodocumento(id) ON DELETE RESTRICT;

ALTER TABLE athena.versaodocumento
    ADD CONSTRAINT fkversaodocumentopedidoajusteorigem
    FOREIGN KEY (pedidoajusteorigemid) REFERENCES athena.pedidoajuste(id) ON DELETE RESTRICT;

CREATE INDEX idxversaodocumentoanterior ON athena.versaodocumento (versaoanteriorid);

CREATE OR REPLACE FUNCTION athena.impediralteracaoversaoconfirmada()
RETURNS trigger AS $$
BEGIN
    IF OLD.confirmada IS TRUE AND (
        OLD.numero IS DISTINCT FROM NEW.numero OR
        OLD.conteudooriginal IS DISTINCT FROM NEW.conteudooriginal OR
        OLD.hashconteudo IS DISTINCT FROM NEW.hashconteudo OR
        OLD.versaoanteriorid IS DISTINCT FROM NEW.versaoanteriorid OR
        OLD.pedidoajusteorigemid IS DISTINCT FROM NEW.pedidoajusteorigemid
    ) THEN
        RAISE EXCEPTION 'versao documental confirmada e imutavel';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trgimpediralteracaoversaoconfirmada
BEFORE UPDATE ON athena.versaodocumento
FOR EACH ROW
EXECUTE FUNCTION athena.impediralteracaoversaoconfirmada();

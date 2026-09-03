ALTER TABLE athena.versaodocumento
    ADD COLUMN id_versao_anterior UUID REFERENCES athena.versaodocumento(id) ON DELETE RESTRICT,
    ADD COLUMN confirmada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN id_pedido_ajuste_origem UUID;

ALTER TABLE athena.pedidoajuste
    ADD COLUMN responsaveltecnico VARCHAR(120) NOT NULL DEFAULT 'responsavel-tecnico-nao-informado',
    ADD COLUMN datatratamento TIMESTAMPTZ,
    ADD COLUMN decisaotratamento VARCHAR(60),
    ADD COLUMN justificativatratamento TEXT,
    ADD COLUMN id_versao_resultante UUID REFERENCES athena.versaodocumento(id) ON DELETE RESTRICT;

ALTER TABLE athena.versaodocumento
    ADD CONSTRAINT ck_versaodocumento_vinculoanterior
    CHECK ((numero = 1 AND id_versao_anterior IS NULL) OR (numero > 1 AND id_versao_anterior IS NOT NULL));

ALTER TABLE athena.versaodocumento
    ADD CONSTRAINT fk_versaodocumento_pedidoajusteorigem
    FOREIGN KEY (id_pedido_ajuste_origem) REFERENCES athena.pedidoajuste(id) ON DELETE RESTRICT;

CREATE INDEX idx_versaodocumento_anterior ON athena.versaodocumento (id_versao_anterior);

CREATE OR REPLACE FUNCTION athena.impediralteracaoversaoconfirmada()
RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.confirmada IS TRUE THEN
            RAISE EXCEPTION 'versao documental confirmada e imutavel';
        END IF;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF OLD.confirmada IS TRUE AND (
            NEW.confirmada IS DISTINCT FROM TRUE OR
            OLD.numero IS DISTINCT FROM NEW.numero OR
            OLD.conteudooriginal IS DISTINCT FROM NEW.conteudooriginal OR
            OLD.hashconteudo IS DISTINCT FROM NEW.hashconteudo OR
            OLD.id_versao_anterior IS DISTINCT FROM NEW.id_versao_anterior OR
            OLD.id_pedido_ajuste_origem IS DISTINCT FROM NEW.id_pedido_ajuste_origem
        ) THEN
            RAISE EXCEPTION 'versao documental confirmada e imutavel';
        END IF;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trgimpediralteracaoversaoconfirmada
BEFORE UPDATE OR DELETE ON athena.versaodocumento
FOR EACH ROW
EXECUTE FUNCTION athena.impediralteracaoversaoconfirmada();

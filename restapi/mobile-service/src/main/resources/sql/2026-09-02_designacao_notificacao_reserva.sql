ALTER TABLE diariooficial.designacaoconhecida
    ADD COLUMN IF NOT EXISTS datareservaprocessamento TIMESTAMP;

ALTER TABLE diariooficial.designacaoconhecida
    ADD COLUMN IF NOT EXISTS identificadorreserva VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_designacaoconhecida_reserva
    ON diariooficial.designacaoconhecida (
        statusnotificacao,
        quantidadetentativas,
        datareservaprocessamento
    );

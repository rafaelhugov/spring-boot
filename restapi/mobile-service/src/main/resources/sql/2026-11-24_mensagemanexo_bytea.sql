ALTER TABLE diariooficial.mensagemanexo
    ADD COLUMN IF NOT EXISTS conteudobytes BYTEA,
    ADD COLUMN IF NOT EXISTS thumbnailbytes BYTEA;

ALTER TABLE curso.inscricao
    ADD COLUMN IF NOT EXISTS pcd boolean NOT NULL DEFAULT false;

ALTER TABLE curso.inscricao
    ADD COLUMN IF NOT EXISTS necessidadeespecial varchar(500);

ALTER TABLE curso.curso ADD COLUMN IF NOT EXISTS status VARCHAR(1);
UPDATE curso.curso SET status = 'P' WHERE status IS NULL;

ALTER TABLE curso.solicitacaocurso
    ADD COLUMN IF NOT EXISTS criteriosespecificospublicoalvo text;

ALTER TABLE curso.curso
    ADD COLUMN IF NOT EXISTS criteriosespecificospublicoalvo text;

ALTER TABLE curso.inscricao
    ADD COLUMN IF NOT EXISTS cienciacriteriospublicoalvo boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS datacienciacriteriospublicoalvo timestamp without time zone,
    ADD COLUMN IF NOT EXISTS criteriospublicoalvodeclarados text;

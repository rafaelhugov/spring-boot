-- Migration V101: Adicionando novos campos na tabela chamada

ALTER TABLE curso.chamada ADD COLUMN IF NOT EXISTS tipo VARCHAR(50);
ALTER TABLE curso.chamada ADD COLUMN IF NOT EXISTS aberta BOOLEAN DEFAULT TRUE;

-- Atualizar registros existentes (para não quebrar a constraint de NOT NULL)
-- Assumimos que chamadas passadas foram de 'INICIO' e já estão fechadas (aberta = false)
UPDATE curso.chamada SET tipo = 'INICIO' WHERE tipo IS NULL;
UPDATE curso.chamada SET aberta = FALSE WHERE aberta IS NULL;

-- Aplicar a constraint NOT NULL
ALTER TABLE curso.chamada ALTER COLUMN tipo SET NOT NULL;
ALTER TABLE curso.chamada ALTER COLUMN aberta SET NOT NULL;

-- Migration V102: Adicionando coluna id_arquivocertificado na tabela curso

ALTER TABLE curso.curso ADD COLUMN IF NOT EXISTS id_arquivocertificado BIGINT;

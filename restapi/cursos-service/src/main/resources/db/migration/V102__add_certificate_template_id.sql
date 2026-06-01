-- Migration V102: Adicionando coluna id_arquivo_certificado na tabela curso

ALTER TABLE curso.curso ADD COLUMN IF NOT EXISTS id_arquivo_certificado BIGINT;

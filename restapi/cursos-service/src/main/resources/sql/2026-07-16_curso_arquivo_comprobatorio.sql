CREATE TABLE IF NOT EXISTS curso.cursoarquivocomprobatorio (
    id SERIAL PRIMARY KEY,
    id_curso INTEGER NOT NULL,
    id_arquivo BIGINT NOT NULL,
    dataregistro TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    FOREIGN KEY (id_curso) REFERENCES curso.curso(id) ON DELETE CASCADE,
    FOREIGN KEY (id_arquivo) REFERENCES ged.arquivo(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cursoarquivocomprobatorio_curso
    ON curso.cursoarquivocomprobatorio (id_curso);

CREATE INDEX IF NOT EXISTS idx_cursoarquivocomprobatorio_arquivo
    ON curso.cursoarquivocomprobatorio (id_arquivo);

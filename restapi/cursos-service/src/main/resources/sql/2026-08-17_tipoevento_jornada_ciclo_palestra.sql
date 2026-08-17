INSERT INTO curso.tipoevento (descricao)
SELECT 'Jornada'
WHERE NOT EXISTS (
    SELECT 1
    FROM curso.tipoevento
    WHERE lower(descricao) = lower('Jornada')
);

INSERT INTO curso.tipoevento (descricao)
SELECT 'Ciclo de Palestra'
WHERE NOT EXISTS (
    SELECT 1
    FROM curso.tipoevento
    WHERE lower(descricao) = lower('Ciclo de Palestra')
);

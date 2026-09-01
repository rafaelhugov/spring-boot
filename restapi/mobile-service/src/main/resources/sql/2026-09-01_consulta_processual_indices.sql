-- Indices recomendados para a consulta publica de processos.
-- Aplicar fora de transacao, preferencialmente em janela monitorada.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_protocolomovimento_consulta_atividade
    ON protocolo.protocolomovimento (
        id_movimento,
        id_protocolo
    )
    WHERE dataexclusao IS NULL
      AND id_protocolomovimento IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_movimentos_consulta_cod_movimento
    ON taxonomia.movimentos (
        cod_movimento,
        id_item
    );

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_protocolo_consulta_publica_data
    ON protocolo.protocolo (
        tipoprotocolo,
        dataregistro DESC,
        id
    )
    WHERE id_pasta IS NULL
      AND dataexclusao IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_protocolo_consulta_publica_comarca
    ON protocolo.protocolo (
        id_orgaocriador,
        dataregistro DESC,
        id
    )
    WHERE tipoprotocolo = 'P'
      AND id_pasta IS NULL
      AND dataexclusao IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_protocolo_consulta_publica_classe
    ON protocolo.protocolo (
        id_classe,
        dataregistro DESC,
        id
    )
    WHERE tipoprotocolo = 'P'
      AND id_pasta IS NULL
      AND dataexclusao IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_protocolo_consulta_publica_numorigem
    ON protocolo.protocolo (
        numeroorigem
    )
    WHERE tipoprotocolo = 'P'
      AND id_pasta IS NULL
      AND dataexclusao IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pasta_consulta_protocolo
    ON protocolo.pasta (
        id_protocolo,
        id_pastadocumentopecainicial
    );

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pastadocumento_consulta_peca
    ON protocolo.pastadocumento (
        id_protocolomovimento,
        id_documento
    );

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_interessado_consulta_protocolo_publico
    ON protocolo.interessado (
        id_protocolo,
        id_pessoa
    )
    WHERE sigiloso = false
      AND dataexclusao IS NULL;

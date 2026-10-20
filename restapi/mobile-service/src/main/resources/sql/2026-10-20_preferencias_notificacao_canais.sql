-- Executar após 2026-10-16_preferencias_notificacao.sql nos bancos MPE e ATENDIMENTO.
-- Mantém as escolhas já realizadas nos dois novos canais.

ALTER TABLE diariooficial.preferencianotificacao
    ADD COLUMN IF NOT EXISTS push_noticias BOOLEAN,
    ADD COLUMN IF NOT EXISTS push_designacoes BOOLEAN,
    ADD COLUMN IF NOT EXISTS push_contracheques BOOLEAN,
    ADD COLUMN IF NOT EXISTS push_pagamentos BOOLEAN,
    ADD COLUMN IF NOT EXISTS push_afastamentos BOOLEAN,
    ADD COLUMN IF NOT EXISTS central_noticias BOOLEAN,
    ADD COLUMN IF NOT EXISTS central_designacoes BOOLEAN,
    ADD COLUMN IF NOT EXISTS central_contracheques BOOLEAN,
    ADD COLUMN IF NOT EXISTS central_pagamentos BOOLEAN,
    ADD COLUMN IF NOT EXISTS central_afastamentos BOOLEAN;

UPDATE diariooficial.preferencianotificacao
SET push_noticias = COALESCE(push_noticias, noticias),
    push_designacoes = COALESCE(push_designacoes, designacoes),
    push_contracheques = COALESCE(push_contracheques, contracheques),
    push_pagamentos = COALESCE(push_pagamentos, pagamentos),
    push_afastamentos = COALESCE(push_afastamentos, afastamentos),
    central_noticias = COALESCE(central_noticias, noticias),
    central_designacoes = COALESCE(central_designacoes, designacoes),
    central_contracheques = COALESCE(central_contracheques, contracheques),
    central_pagamentos = COALESCE(central_pagamentos, pagamentos),
    central_afastamentos = COALESCE(central_afastamentos, afastamentos);

ALTER TABLE diariooficial.preferencianotificacao
    ALTER COLUMN push_noticias SET DEFAULT TRUE, ALTER COLUMN push_noticias SET NOT NULL,
    ALTER COLUMN push_designacoes SET DEFAULT TRUE, ALTER COLUMN push_designacoes SET NOT NULL,
    ALTER COLUMN push_contracheques SET DEFAULT TRUE, ALTER COLUMN push_contracheques SET NOT NULL,
    ALTER COLUMN push_pagamentos SET DEFAULT TRUE, ALTER COLUMN push_pagamentos SET NOT NULL,
    ALTER COLUMN push_afastamentos SET DEFAULT TRUE, ALTER COLUMN push_afastamentos SET NOT NULL,
    ALTER COLUMN central_noticias SET DEFAULT TRUE, ALTER COLUMN central_noticias SET NOT NULL,
    ALTER COLUMN central_designacoes SET DEFAULT TRUE, ALTER COLUMN central_designacoes SET NOT NULL,
    ALTER COLUMN central_contracheques SET DEFAULT TRUE, ALTER COLUMN central_contracheques SET NOT NULL,
    ALTER COLUMN central_pagamentos SET DEFAULT TRUE, ALTER COLUMN central_pagamentos SET NOT NULL,
    ALTER COLUMN central_afastamentos SET DEFAULT TRUE, ALTER COLUMN central_afastamentos SET NOT NULL;

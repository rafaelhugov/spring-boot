# Concessao de Folga

## Fonte remota

A feature consulta dados do banco ATENDIMENTO exclusivamente pelo backend.
O aplicativo Flutter nao acessa esse banco e nao envia matricula arbitraria.

A conexao segue o mesmo padrao usado pelo Contracheque: o `mobile-service` usa o datasource principal
configurado por variaveis de ambiente (`PG_URL`, `PG_USERNAME`, `PG_PASSWORD`). Nos ambientes onde esta
feature estiver habilitada, esse datasource deve apontar para ATENDIMENTO.

Nao ha credenciais fixas no codigo e o aplicativo nao recebe detalhes internos da conexao.

## Views

- Concessoes vigentes: `recursoshumanos.concessoes_vigentes_vw`
- Calendario de afastamentos: `relatorios.ausenciasdemembroseservidores_vw`

O filtro mensal de ausencias usa intersecao entre intervalos:

- inicio do afastamento menor ou igual ao ultimo dia do mes consultado;
- fim do afastamento maior ou igual ao primeiro dia do mes consultado.

## Regras confirmadas

- `L` na view de concessoes representa exclusivamente licenca-premio.
- `E` representa licenca eleitoral.
- `F` representa ferias.
- `P` representa plantao.

## Pendencias

- Confirmar oficialmente o mapeamento de `periodo_concessao` para licenca eleitoral:
  `1 = Treinamento`, `2 = Primeiro turno`, `3 = Segundo turno`.
- Definir a origem de afastamentos por diarias; a view de concessoes ainda nao mapeia esse tipo.
- Avaliar inclusao futura de subtipo de concessao caso a view passe a retornar mais tipos de licenca.

## Desenvolvimento

No `.env.dev`, `PG_URL` deve apontar para ATENDIMENTO para que as views sejam consultadas localmente,
igual ao fluxo de Contracheque.

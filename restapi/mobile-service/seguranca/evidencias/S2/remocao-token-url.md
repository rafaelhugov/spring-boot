# S2 - Evidencia inicial de eliminacao de tokens em URL

Data: 2026-09-24
Branches:
- Backend: `seguranca/s2-eliminar-tokens-url`
- Flutter: `seguranca/s2-eliminar-tokens-url`

## Mudanca

- Backend passou a aceitar `Authorization: Bearer <token>` nos endpoints sensiveis migrados, mantendo `token` em query como compatibilidade transitoria.
- Flutter deixou de enviar `token` em query string nos modulos sensiveis migrados.
- URLs de foto autenticada e PDF de contracheque deixaram de incluir token de sessao.
- Mídia autenticada no Flutter passou a enviar Bearer em header quando carrega foto/PDF.

## Endpoints/modulos cobertos

- Perfil
- Carteira funcional dados/foto
- Designacoes
- Contracheque dados/PDF
- Pagamentos
- Recursos humanos/concessoes/afastamentos

## Evidencia de scan

Comando:

```bash
rg -n "token=|queryParameters: \\{'token'|headers: \\{'Authorization': 'Token|RequestParam\\(\\\"token\\\"\\)" app_mppb_flutter/lib spring-boot/restapi/mobile-service/src/main/java -g '*.dart' -g '*.java'
```

Resultado:
- Sem ocorrencias.

## Testes

Backend:

```bash
mvn -q test
```

Resultado:
- Passou.

Flutter:

```bash
flutter test
```

Resultado:
- Passou, 110 testes.
- Observacao recorrente: 69 pacotes com versoes novas incompativeis com constraints atuais.

## Compatibilidade e risco residual

- Compatibilidade transitoria com query `token` ainda existe no backend por desenho da S2-T01.
- S2 ainda precisa prosseguir para bloquear query legado no backend em S2-T10, depois de validar consumidores.
- `DispositivoNotificacaoController` ja exigia Bearer e continua rejeitando query token.

## Rollback

- Backend: reverter o commit da S2 restaura assinaturas antigas dos controllers.
- Flutter: reverter o commit da S2 restaura query token nos clientes, enquanto o backend ainda aceita compatibilidade.


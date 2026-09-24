# S1-T09 - Evidencia de baseline

## Flutter

Comando executado em `/home/rafaelhugo/projetosMPPB/app_mppb_flutter`:

```bash
flutter test
```

Resultado:
- Primeira execucao dentro do sandbox falhou porque o SDK Flutter precisava escrever no proprio cache fora da area gravavel.
- Execucao aprovada fora do sandbox.
- Resultado final: `All tests passed!`
- Total observado: 110 testes aprovados.
- Observacao: o comando reportou 69 pacotes com versoes novas incompativeis com as constraints atuais.

## Backend

Comando executado em `/home/rafaelhugo/projetosMPPB/spring-boot/restapi/mobile-service`:

```bash
mvn -q test
```

Resultado:
- Build/testes concluidos com codigo 0.
- Logs de teste mostram cenarios de notificacao/designacao/FCM e controllers.

## Contagem de arquivos de teste

```bash
find app_mppb_flutter/test -type f | wc -l
find spring-boot/restapi/mobile-service/src/test/java -type f | wc -l
```

Resultado:
- Flutter: 18 arquivos.
- Backend: 17 arquivos.


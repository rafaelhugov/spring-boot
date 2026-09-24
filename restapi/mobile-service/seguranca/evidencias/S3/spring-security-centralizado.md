# S3 - Spring Security centralizado

Data: 2026-09-24

Branch: `seguranca/s3-spring-security-autorizacao`

## Objetivo

Centralizar autenticação/autorização dos endpoints sensíveis no Spring Security, removendo validação manual de token em controllers e reduzindo `PUBLIC_URLS` ao público real.

## Alterações executadas

- `restapi-common`
  - `CustomBasicAuthenticationFilter` passou a aceitar `Authorization: Bearer <token>` e manteve compatibilidade com `Authorization: Token <token>`.
  - `SecurityConfig` removeu de `PUBLIC_URLS` rotas funcionais sensíveis de carteira, contracheque, férias/RH, designações, notificações e pagamentos.
  - Resposta 401 padronizada para `{"error":"Unauthorized"}`, sem refletir detalhe interno da exceção.

- `mobile-service`
  - Novo helper `AutenticacaoAtual` para recuperar `login` e `token` do `SecurityContext`.
  - Controllers migrados para identidade centralizada:
    - `CarteiraFuncionalController`
    - `ConcessaoFolgaController`
    - `ContrachequeController`
    - `DesignacaoController`
    - `DispositivoNotificacaoController`
    - `PagamentosController`
    - `PerfilUsuarioController`
  - Removido resolvedor legado de token por query/header manual.

## Validação

Comando executado:

```bash
mvn -q -pl restapi-common,mobile-service test
```

Resultado: aprovado.

Varredura executada:

```bash
rg -n "RequestParam\(value = \"token\"|RequestHeader\(value = \"Authorization\"|ResolvedorTokenAutenticacao|tokenLegado|extrairBearerToken|extractLoginFromToken|getLoginByToken" src/main/java ../restapi-common/src/main/java
```

Resultado: sem ocorrências.

## Testes adicionados/ajustados

- `CustomBasicAuthenticationFilterTest`
  - autenticação por `Bearer`;
  - compatibilidade com `Token`;
  - 401 sanitizado para token inválido.
- `DesignacaoControllerTest`
  - uso do login vindo do `SecurityContext`;
  - negativo sem contexto;
  - negativo sem matrícula.
- `ConcessaoFolgaControllerTest`
  - token/login obtidos do `SecurityContext`;
  - preservação do modo `mpvirtual.recursos-humanos.auth-mode`.
- `DispositivoNotificacaoControllerTest`
  - registro/desativação com identidade centralizada;
  - negativos sem autenticação;
  - query token não autentica.

## Risco tratado

- A-02: rotas sensíveis em `PUBLIC_URLS` e autenticação manual em controllers.
- Redução de bypass por esquecimento de validação manual em endpoint novo.
- Redução de divergência entre endpoints quanto a 401/403 e transporte de credencial.

## Rollback

Reverter o commit da S3 restaura:

- `PUBLIC_URLS` anterior;
- validação manual nos controllers;
- resolvedor legado de token;
- filtro aceitando apenas `Authorization: Token`.

## Pendências para fases futuras

- S4 deve validar configurações de produção e falha segura de ambiente.
- S5 deve revisar sessão/biometria e ciclo de credenciais.
- S7 deve transformar estas validações em gates de pipeline.

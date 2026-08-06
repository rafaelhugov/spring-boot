# 🛡️ NCAP - API Backend (Spring Boot)

API RESTful para o **NCAP - Núcleo de Controle da Atividade Policial**, desenvolvida em **Java 21** e **Spring Boot 3.4.4**.

---

## 📋 Pré-requisitos

- **Java 21 (JDK)**
- **Maven 3.9+**
- **PostgreSQL 15+** (ou ambiente em memória para testes)

---

## ⚙️ Configuração de Ambiente

Copie o arquivo de modelo `.env_tpl` para `.env` e configure as variáveis locais:

```bash
cp .env_tpl .env
```

### Variáveis Chave:
- `SPRING_DATASOURCE_URL`: URL de conexão PostgreSQL
- `SPRING_DATASOURCE_USERNAME`: Usuário do banco
- `SPRING_DATASOURCE_PASSWORD`: Senha do banco
- `SECURITY_MOCK_AUTH`: Define se o bypass de autenticação para desenvolvimento local está ativo (`true`/`false`)

---

## 🚀 Como Executar

### 1. Compilar e Testar

```bash
mvn clean test
```

### 2. Executar Localmente

```bash
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8080/api`.

---

## 🐳 Execução via Docker

```bash
docker build -t ncap-api .
docker run -p 8080:8080 --env-file .env ncap-api
```

---

## 🔒 Segurança & Controle de Acesso (RBAC)

A API utiliza anotações `@PreAuthorize` com permissões baseadas no padrao MPPB:
- `ceap.acesso`: Acesso geral e leitura de dados públicos/auxiliares.
- `ceap.inspecoes`: Gerenciamento do ciclo de vida das inspeções.
- `ceap.manter.entidades`: Administração e sincronização de delegacias e órgãos de perícia.

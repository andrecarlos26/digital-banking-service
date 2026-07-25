# Di2win Digital Account API

API REST desenvolvida em **Java 21**, **Spring Boot**, **PostgreSQL** e **Flyway** para gerenciar clientes, contas digitais, depósitos, saques, bloqueios e extratos.

## Principais decisões

- Arquitetura modular por domínio (`customer` e `account`), mantendo API, serviço, domínio e repositório próximos.
- Valores monetários com `BigDecimal` e escala de duas casas decimais.
- CPF normalizado para 11 dígitos, validado pelos dígitos verificadores e protegido por restrição única no banco.
- Uma conta por cliente, criada a partir do CPF.
- Operações financeiras serializadas com bloqueio pessimista no banco, evitando perda de atualização concorrente.
- Saldo protegido por regra de domínio e `CHECK CONSTRAINT` no banco.
- Limite diário de saque configurável e calculado no fuso de negócio.
- Exclusão lógica do cliente para preservar histórico; uma conta com saldo positivo impede a remoção.
- Migrações versionadas com Flyway.
- Respostas de erro padronizadas em `application/problem+json`.
- Documentação OpenAPI/Swagger, Actuator, Docker, Postman e testes automatizados.

## Tecnologias

- Java 21
- Spring Boot 3.5.16
- Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Flyway
- Bean Validation
- springdoc-openapi / Swagger UI
- JUnit 5, MockMvc e H2
- Maven, JaCoCo e Docker Compose

## Executando com Docker

```bash
cp .env.example .env
docker compose up --build
```

A API ficará disponível em `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/actuator/health`

## Executando localmente

Requisitos: Java 21 e Docker para o PostgreSQL.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

## Testes

```bash
./mvnw clean verify
```

O relatório de cobertura é gerado em `target/site/jacoco/index.html`.

## Configuração

| Variável | Padrão | Descrição |
|---|---:|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/di2win_accounts` | URL JDBC |
| `DB_USERNAME` | `di2win` | Usuário do banco |
| `DB_PASSWORD` | `di2win` | Senha do banco |
| `DAILY_WITHDRAWAL_LIMIT` | `2000.00` | Limite diário de saque em R$ |
| `BUSINESS_TIMEZONE` | `America/Sao_Paulo` | Fuso usado para fechar o dia financeiro |
| `ACCOUNT_AGENCY` | `0001` | Agência atribuída às novas contas |

## Endpoints

### Clientes

| Método | Rota | Função |
|---|---|---|
| `POST` | `/api/v1/customers` | Cria um cliente |
| `GET` | `/api/v1/customers/{id}` | Consulta um cliente ativo |
| `DELETE` | `/api/v1/customers/{id}` | Remove logicamente um cliente e encerra sua conta sem saldo |

### Contas

| Método | Rota | Função |
|---|---|---|
| `POST` | `/api/v1/accounts` | Cria uma conta usando o CPF |
| `GET` | `/api/v1/accounts/{accountNumber}` | Consulta os dados da conta |
| `GET` | `/api/v1/accounts/{accountNumber}/balance` | Consulta o saldo |
| `PATCH` | `/api/v1/accounts/{accountNumber}/block` | Bloqueia a conta |
| `PATCH` | `/api/v1/accounts/{accountNumber}/unblock` | Desbloqueia a conta |
| `POST` | `/api/v1/accounts/{accountNumber}/deposits` | Efetua depósito |
| `POST` | `/api/v1/accounts/{accountNumber}/withdrawals` | Efetua saque |
| `GET` | `/api/v1/accounts/{accountNumber}/statement` | Emite extrato por período |

## Fluxo rápido com cURL

### 1. Criar cliente

```bash
curl -X POST http://localhost:8080/api/v1/customers \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "André Carlos",
    "cpf": "529.982.247-25",
    "birthDate": "1995-05-20"
  }'
```

### 2. Criar conta

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"cpf": "529.982.247-25"}'
```

Copie o campo `number` retornado e use nos comandos seguintes.

### 3. Depositar

```bash
curl -X POST http://localhost:8080/api/v1/accounts/NUMERO_DA_CONTA/deposits \
  -H 'Content-Type: application/json' \
  -d '{"amount": 2500.00, "description": "Depósito inicial"}'
```

### 4. Sacar

```bash
curl -X POST http://localhost:8080/api/v1/accounts/NUMERO_DA_CONTA/withdrawals \
  -H 'Content-Type: application/json' \
  -d '{"amount": 500.00, "description": "Saque"}'
```

### 5. Extrato

```bash
curl 'http://localhost:8080/api/v1/accounts/NUMERO_DA_CONTA/statement?startDate=2026-07-01&endDate=2026-07-31&page=0&size=20'
```

## Regras de negócio

1. CPF deve ser válido e único.
2. Cliente removido não pode abrir conta nem ser consultado como ativo.
3. Cada cliente pode possuir uma única conta.
4. Depósitos e saques exigem conta ativa e desbloqueada.
5. Saques exigem saldo suficiente e não podem ultrapassar o limite diário acumulado.
6. O saldo nunca pode ficar negativo.
7. Bloquear/desbloquear é idempotente.
8. O extrato aceita no máximo 366 dias e retorna saldo inicial, totais, saldo final e paginação.
9. Cliente com saldo positivo não pode ser removido.

Mais detalhes estão em [`docs/architecture.md`](docs/architecture.md), [`docs/database-model.md`](docs/database-model.md) e [`docs/api-examples.md`](docs/api-examples.md).

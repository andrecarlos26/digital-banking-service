# Rastreabilidade dos requisitos

| Requisito do desafio | Implementação |
|---|---|
| Criar cliente | `POST /api/v1/customers` |
| Remover cliente | `DELETE /api/v1/customers/{id}` com exclusão lógica |
| Nome, CPF e data de nascimento | Entidade `Customer` e `CreateCustomerRequest` |
| CPF com dígitos verificadores | `@ValidCpf` e `CpfValidator` |
| CPF único | Validação de serviço e `UNIQUE (cpf)` |
| Criar conta pelo CPF | `POST /api/v1/accounts` |
| Saldo, número e agência consultáveis | `GET /api/v1/accounts/{number}` e `/balance` |
| Depósito | `POST /api/v1/accounts/{number}/deposits` |
| Saque | `POST /api/v1/accounts/{number}/withdrawals` |
| Conta ativa e desbloqueada | Regras na entidade `DigitalAccount` |
| Saldo suficiente | Validação atômica em `DigitalAccount.withdraw` |
| Limite diário configurável | `DAILY_WITHDRAWAL_LIMIT` e consulta do acumulado diário |
| Saldo nunca negativo | Regra de domínio e constraint `ck_accounts_balance_non_negative` |
| Bloquear conta | `PATCH /api/v1/accounts/{number}/block` |
| Desbloquear conta | `PATCH /api/v1/accounts/{number}/unblock` |
| Extrato por período | `GET /api/v1/accounts/{number}/statement` |
| Banco relacional | PostgreSQL com migrações Flyway |
| Script de banco | `src/main/resources/db/migration/V1__create_tables.sql` |
| Documentação | README, Swagger/OpenAPI, documentos em `docs/` e Postman |
| Testes | Testes unitários e de integração em `src/test` |
| Boas práticas | Transações, lock pessimista, DTOs, Problem Details, configuração externa e CI |

# Modelo de dados

```mermaid
erDiagram
    CUSTOMERS ||--o| ACCOUNTS : owns
    ACCOUNTS ||--o{ ACCOUNT_TRANSACTIONS : records

    CUSTOMERS {
        uuid id PK
        varchar name
        char cpf UK
        date birth_date
        boolean active
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }

    ACCOUNTS {
        uuid id PK
        uuid customer_id FK,UK
        varchar number UK
        varchar agency
        numeric balance
        varchar status
        boolean blocked
        bigint version
        timestamptz created_at
        timestamptz updated_at
        timestamptz closed_at
    }

    ACCOUNT_TRANSACTIONS {
        uuid id PK
        uuid account_id FK
        varchar type
        numeric amount
        numeric balance_after
        varchar description
        timestamptz occurred_at
    }
```

## Restrições relevantes

- CPF único e armazenado sem pontuação.
- Uma conta por cliente (`UNIQUE customer_id`).
- Número da conta único.
- Saldo e saldo resultante nunca negativos.
- Valor da transação estritamente positivo.
- Transações não são apagadas em cascata.

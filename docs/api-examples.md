# Exemplos da API

## Erro padronizado

```json
{
  "type": "about:blank",
  "title": "Limite diário de saque excedido",
  "status": 422,
  "detail": "O saque excede o limite diário configurado de R$ 2000.00.",
  "instance": "/api/v1/accounts/12345678/withdrawals",
  "code": "DAILY_WITHDRAWAL_LIMIT_EXCEEDED",
  "timestamp": "2026-07-24T21:00:00Z"
}
```

## Criar cliente

```json
{
  "name": "Maria Silva",
  "cpf": "111.444.777-35",
  "birthDate": "1990-01-10"
}
```

## Criar conta

```json
{
  "cpf": "111.444.777-35"
}
```

## Depósito ou saque

```json
{
  "amount": 100.00,
  "description": "Operação via aplicativo"
}
```

## Extrato

`GET /api/v1/accounts/{number}/statement?startDate=2026-07-01&endDate=2026-07-31&page=0&size=20`

O período é inclusivo nas duas datas e interpretado no fuso de negócio.

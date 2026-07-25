package br.com.di2win.digitalaccount.account.api.dto;

import br.com.di2win.digitalaccount.account.domain.AccountTransaction;
import br.com.di2win.digitalaccount.account.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        String accountNumber,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String currency,
        String description,
        Instant occurredAt
) {
    public static TransactionResponse from(AccountTransaction transaction, String accountNumber) {
        return new TransactionResponse(
                transaction.getId(),
                accountNumber,
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                "BRL",
                transaction.getDescription(),
                transaction.getOccurredAt()
        );
    }
}

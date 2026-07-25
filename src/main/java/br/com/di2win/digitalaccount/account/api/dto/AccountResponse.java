package br.com.di2win.digitalaccount.account.api.dto;

import br.com.di2win.digitalaccount.account.domain.AccountStatus;
import br.com.di2win.digitalaccount.account.domain.DigitalAccount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String number,
        String agency,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        boolean blocked,
        BigDecimal dailyWithdrawalLimit,
        UUID customerId,
        String customerName,
        Instant createdAt
) {
    public static AccountResponse from(DigitalAccount account, BigDecimal dailyWithdrawalLimit) {
        return new AccountResponse(
                account.getId(),
                account.getNumber(),
                account.getAgency(),
                account.getBalance(),
                "BRL",
                account.getStatus(),
                account.isBlocked(),
                dailyWithdrawalLimit,
                account.getCustomer().getId(),
                account.getCustomer().getName(),
                account.getCreatedAt()
        );
    }
}

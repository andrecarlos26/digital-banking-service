package br.com.di2win.digitalaccount.account.api.dto;

import br.com.di2win.digitalaccount.account.domain.AccountStatus;
import br.com.di2win.digitalaccount.account.domain.DigitalAccount;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
        String accountNumber,
        String agency,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        boolean blocked,
        Instant asOf
) {
    public static BalanceResponse from(DigitalAccount account, Instant asOf) {
        return new BalanceResponse(
                account.getNumber(),
                account.getAgency(),
                account.getBalance(),
                "BRL",
                account.getStatus(),
                account.isBlocked(),
                asOf
        );
    }
}

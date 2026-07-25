package br.com.di2win.digitalaccount.account.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StatementResponse(
        String accountNumber,
        String agency,
        String currency,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal openingBalance,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        BigDecimal closingBalance,
        BigDecimal currentBalance,
        Instant generatedAt,
        PageInfo page,
        List<TransactionResponse> transactions
) {
    public record PageInfo(
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }
}

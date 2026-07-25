package br.com.di2win.digitalaccount.account.repository;

import br.com.di2win.digitalaccount.account.domain.AccountTransaction;
import br.com.di2win.digitalaccount.account.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {

    @Query("""
            select sum(t.amount)
            from AccountTransaction t
            where t.account.id = :accountId
              and t.type = :type
              and t.occurredAt >= :start
              and t.occurredAt < :end
            """)
    BigDecimal sumAmountByTypeAndPeriod(
            @Param("accountId") UUID accountId,
            @Param("type") TransactionType type,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}

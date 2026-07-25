package br.com.di2win.digitalaccount.account.repository;

import br.com.di2win.digitalaccount.account.domain.DigitalAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DigitalAccountRepository extends JpaRepository<DigitalAccount, UUID> {

    boolean existsByNumber(String number);

    boolean existsByCustomerId(UUID customerId);

    Optional<DigitalAccount> findByNumber(String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from DigitalAccount a where a.number = :number")
    Optional<DigitalAccount> findByNumberForUpdate(@Param("number") String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from DigitalAccount a where a.customer.id = :customerId")
    Optional<DigitalAccount> findByCustomerIdForUpdate(@Param("customerId") UUID customerId);
}

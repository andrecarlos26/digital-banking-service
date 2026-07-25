package br.com.di2win.digitalaccount.account.repository;

import br.com.di2win.digitalaccount.account.domain.DigitalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DigitalAccountRepository extends JpaRepository<DigitalAccount, UUID> {

    boolean existsByNumber(String number);

    boolean existsByCustomerId(UUID customerId);

    Optional<DigitalAccount> findByNumber(String number);
}

package br.com.di2win.digitalaccount.account.repository;

import br.com.di2win.digitalaccount.account.domain.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
}

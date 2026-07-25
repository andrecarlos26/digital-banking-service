package br.com.di2win.digitalaccount.account.service;

import br.com.di2win.digitalaccount.account.repository.DigitalAccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final int MAX_ATTEMPTS = 20;

    private final DigitalAccountRepository accountRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountNumberGenerator(DigitalAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String number = "%08d".formatted(random.nextInt(100_000_000));
            if (!accountRepository.existsByNumber(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Unable to generate a unique account number");
    }
}

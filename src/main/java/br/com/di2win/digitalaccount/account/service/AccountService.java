package br.com.di2win.digitalaccount.account.service;

import br.com.di2win.digitalaccount.account.api.dto.AccountResponse;
import br.com.di2win.digitalaccount.account.api.dto.BalanceResponse;
import br.com.di2win.digitalaccount.account.api.dto.CreateAccountRequest;
import br.com.di2win.digitalaccount.account.api.dto.MoneyOperationRequest;
import br.com.di2win.digitalaccount.account.api.dto.TransactionResponse;
import br.com.di2win.digitalaccount.account.domain.AccountTransaction;
import br.com.di2win.digitalaccount.account.domain.DigitalAccount;
import br.com.di2win.digitalaccount.account.domain.TransactionType;
import br.com.di2win.digitalaccount.account.repository.AccountTransactionRepository;
import br.com.di2win.digitalaccount.account.repository.DigitalAccountRepository;
import br.com.di2win.digitalaccount.common.exception.ApiException;
import br.com.di2win.digitalaccount.common.exception.ErrorCode;
import br.com.di2win.digitalaccount.config.AccountProperties;
import br.com.di2win.digitalaccount.customer.domain.Customer;
import br.com.di2win.digitalaccount.customer.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AccountService {

    private final DigitalAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final CustomerService customerService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AccountProperties properties;
    private final Clock clock;

    public AccountService(
            DigitalAccountRepository accountRepository,
            AccountTransactionRepository transactionRepository,
            CustomerService customerService,
            AccountNumberGenerator accountNumberGenerator,
            AccountProperties properties,
            Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.customerService = customerService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        Customer customer = customerService.findActiveByCpfForUpdate(request.cpf());
        if (accountRepository.existsByCustomerId(customer.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.ACCOUNT_ALREADY_EXISTS,
                    "Conta já existente", "O cliente informado já possui uma conta digital.");
        }

        DigitalAccount account = new DigitalAccount(
                UUID.randomUUID(),
                customer,
                accountNumberGenerator.generate(),
                properties.agency(),
                clock.instant()
        );
        return AccountResponse.from(accountRepository.save(account), properties.dailyWithdrawalLimit());
    }

    @Transactional(readOnly = true)
    public AccountResponse find(String accountNumber) {
        return AccountResponse.from(findByNumber(accountNumber), properties.dailyWithdrawalLimit());
    }

    @Transactional(readOnly = true)
    public BalanceResponse balance(String accountNumber) {
        return BalanceResponse.from(findByNumber(accountNumber), clock.instant());
    }

    @Transactional
    public AccountResponse block(String accountNumber) {
        DigitalAccount account = findByNumberForUpdate(accountNumber);
        account.block(clock.instant());
        return AccountResponse.from(account, properties.dailyWithdrawalLimit());
    }

    @Transactional
    public AccountResponse unblock(String accountNumber) {
        DigitalAccount account = findByNumberForUpdate(accountNumber);
        account.unblock(clock.instant());
        return AccountResponse.from(account, properties.dailyWithdrawalLimit());
    }

    @Transactional
    public TransactionResponse deposit(String accountNumber, MoneyOperationRequest request) {
        DigitalAccount account = findByNumberForUpdate(accountNumber);
        BigDecimal amount = normalizeAmount(request.amount());
        Instant now = clock.instant();
        BigDecimal balanceAfter = account.deposit(amount, now);
        AccountTransaction transaction = new AccountTransaction(
                UUID.randomUUID(),
                account,
                TransactionType.DEPOSIT,
                amount,
                balanceAfter,
                normalizeDescription(request.description()),
                now
        );
        return TransactionResponse.from(transactionRepository.save(transaction), accountNumber);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_AMOUNT,
                    "Valor inválido", "O valor deve possuir no máximo duas casas decimais.");
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim().replaceAll("\\s+", " ");
    }

    private DigitalAccount findByNumber(String accountNumber) {
        return accountRepository.findByNumber(accountNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.ACCOUNT_NOT_FOUND,
                        "Conta não encontrada", "Não existe conta com o número informado."));
    }

    private DigitalAccount findByNumberForUpdate(String accountNumber) {
        return accountRepository.findByNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.ACCOUNT_NOT_FOUND,
                        "Conta não encontrada", "Não existe conta com o número informado."));
    }
}

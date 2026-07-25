package br.com.di2win.digitalaccount.account.service;

import br.com.di2win.digitalaccount.account.api.dto.AccountResponse;
import br.com.di2win.digitalaccount.account.api.dto.BalanceResponse;
import br.com.di2win.digitalaccount.account.api.dto.CreateAccountRequest;
import br.com.di2win.digitalaccount.account.api.dto.MoneyOperationRequest;
import br.com.di2win.digitalaccount.account.api.dto.StatementResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AccountService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final long MAX_STATEMENT_DAYS = 366;

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

    @Transactional
    public TransactionResponse withdraw(String accountNumber, MoneyOperationRequest request) {
        DigitalAccount account = findByNumberForUpdate(accountNumber);
        BigDecimal amount = normalizeAmount(request.amount());
        validateDailyWithdrawalLimit(account, amount);

        Instant now = clock.instant();
        BigDecimal balanceAfter = account.withdraw(amount, now);
        AccountTransaction transaction = new AccountTransaction(
                UUID.randomUUID(),
                account,
                TransactionType.WITHDRAWAL,
                amount,
                balanceAfter,
                normalizeDescription(request.description()),
                now
        );
        return TransactionResponse.from(transactionRepository.save(transaction), accountNumber);
    }

    @Transactional(readOnly = true)
    public StatementResponse statement(
            String accountNumber,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validatePeriod(startDate, endDate);
        DigitalAccount account = findByNumber(accountNumber);

        Instant start = startDate.atStartOfDay(properties.businessTimezone()).toInstant();
        Instant endExclusive = endDate.plusDays(1).atStartOfDay(properties.businessTimezone()).toInstant();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AccountTransaction> transactions = transactionRepository
                .findByAccountIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        account.getId(), start, endExclusive, pageRequest);

        BigDecimal openingBalance = transactionRepository
                .findFirstByAccountIdAndOccurredAtBeforeOrderByOccurredAtDesc(account.getId(), start)
                .map(AccountTransaction::getBalanceAfter)
                .orElse(ZERO);

        BigDecimal totalDeposits = sum(account.getId(), TransactionType.DEPOSIT, start, endExclusive);
        BigDecimal totalWithdrawals = sum(account.getId(), TransactionType.WITHDRAWAL, start, endExclusive);
        BigDecimal closingBalance = openingBalance.add(totalDeposits).subtract(totalWithdrawals);

        StatementResponse.PageInfo pageInfo = new StatementResponse.PageInfo(
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages(),
                transactions.isFirst(),
                transactions.isLast()
        );

        return new StatementResponse(
                account.getNumber(),
                account.getAgency(),
                "BRL",
                startDate,
                endDate,
                openingBalance,
                totalDeposits,
                totalWithdrawals,
                closingBalance,
                account.getBalance(),
                clock.instant(),
                pageInfo,
                transactions.stream().map(tx -> TransactionResponse.from(tx, accountNumber)).toList()
        );
    }

    private void validateDailyWithdrawalLimit(DigitalAccount account, BigDecimal amount) {
        LocalDate businessDate = LocalDate.now(clock);
        Instant start = businessDate.atStartOfDay(properties.businessTimezone()).toInstant();
        Instant end = businessDate.plusDays(1).atStartOfDay(properties.businessTimezone()).toInstant();
        BigDecimal withdrawnToday = sum(account.getId(), TransactionType.WITHDRAWAL, start, end);

        if (withdrawnToday.add(amount).compareTo(properties.dailyWithdrawalLimit()) > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    ErrorCode.DAILY_WITHDRAWAL_LIMIT_EXCEEDED,
                    "Limite diário de saque excedido",
                    "O saque excede o limite diário configurado de R$ "
                            + properties.dailyWithdrawalLimit().setScale(2, RoundingMode.UNNECESSARY) + ".");
        }
    }

    private BigDecimal sum(UUID accountId, TransactionType type, Instant start, Instant end) {
        BigDecimal value = transactionRepository.sumAmountByTypeAndPeriod(accountId, type, start, end);
        return value == null ? ZERO : value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PERIOD,
                    "Período inválido", "A data inicial deve ser anterior ou igual à data final.");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_STATEMENT_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PERIOD,
                    "Período inválido", "O extrato pode abranger no máximo 366 dias.");
        }
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

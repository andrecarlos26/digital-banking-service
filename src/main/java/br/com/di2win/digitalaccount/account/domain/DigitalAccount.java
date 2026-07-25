package br.com.di2win.digitalaccount.account.domain;

import br.com.di2win.digitalaccount.common.exception.ApiException;
import br.com.di2win.digitalaccount.common.exception.ErrorCode;
import br.com.di2win.digitalaccount.customer.domain.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class DigitalAccount {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(nullable = false, unique = true, length = 20)
    private String number;

    @Column(nullable = false, length = 10)
    private String agency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false)
    private boolean blocked;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected DigitalAccount() {
    }

    public DigitalAccount(UUID id, Customer customer, String number, String agency, Instant now) {
        this.id = id;
        this.customer = customer;
        this.number = number;
        this.agency = agency;
        this.balance = new BigDecimal("0.00");
        this.status = AccountStatus.ACTIVE;
        this.blocked = false;
        this.version = 0L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public BigDecimal deposit(BigDecimal amount, Instant now) {
        ensureAvailableForTransactions();
        balance = balance.add(amount);
        updatedAt = now;
        return balance;
    }

    public BigDecimal withdraw(BigDecimal amount, Instant now) {
        ensureAvailableForTransactions();
        if (balance.compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INSUFFICIENT_FUNDS,
                    "Saldo insuficiente", "A conta não possui saldo suficiente para o saque.");
        }
        balance = balance.subtract(amount);
        updatedAt = now;
        return balance;
    }

    public void block(Instant now) {
        ensureActive();
        blocked = true;
        updatedAt = now;
    }

    public void unblock(Instant now) {
        ensureActive();
        blocked = false;
        updatedAt = now;
    }

    public void close(Instant now) {
        if (balance.signum() > 0) {
            throw new IllegalStateException("An account with a positive balance cannot be closed");
        }
        status = AccountStatus.CLOSED;
        blocked = true;
        closedAt = now;
        updatedAt = now;
    }

    private void ensureAvailableForTransactions() {
        ensureActive();
        if (blocked) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.ACCOUNT_BLOCKED,
                    "Conta bloqueada", "A conta está bloqueada para movimentações.");
        }
    }

    private void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.ACCOUNT_INACTIVE,
                    "Conta inativa", "A conta não está ativa.");
        }
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getNumber() {
        return number;
    }

    public String getAgency() {
        return agency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

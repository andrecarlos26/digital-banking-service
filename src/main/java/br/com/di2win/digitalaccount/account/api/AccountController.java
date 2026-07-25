package br.com.di2win.digitalaccount.account.api;

import br.com.di2win.digitalaccount.account.api.dto.AccountResponse;
import br.com.di2win.digitalaccount.account.api.dto.BalanceResponse;
import br.com.di2win.digitalaccount.account.api.dto.CreateAccountRequest;
import br.com.di2win.digitalaccount.account.api.dto.MoneyOperationRequest;
import br.com.di2win.digitalaccount.account.api.dto.StatementResponse;
import br.com.di2win.digitalaccount.account.api.dto.TransactionResponse;
import br.com.di2win.digitalaccount.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
@Tag(name = "Accounts", description = "Contas, saldo, movimentações e extrato")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Cria uma conta a partir do CPF do cliente")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{number}")
                .buildAndExpand(response.number())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Consulta dados da conta")
    public AccountResponse find(@PathVariable String accountNumber) {
        return accountService.find(accountNumber);
    }

    @GetMapping("/{accountNumber}/balance")
    @Operation(summary = "Consulta saldo da conta")
    public BalanceResponse balance(@PathVariable String accountNumber) {
        return accountService.balance(accountNumber);
    }

    @PatchMapping("/{accountNumber}/block")
    @Operation(summary = "Bloqueia a conta")
    public AccountResponse block(@PathVariable String accountNumber) {
        return accountService.block(accountNumber);
    }

    @PatchMapping("/{accountNumber}/unblock")
    @Operation(summary = "Desbloqueia a conta")
    public AccountResponse unblock(@PathVariable String accountNumber) {
        return accountService.unblock(accountNumber);
    }

    @PostMapping("/{accountNumber}/deposits")
    @Operation(summary = "Efetua depósito")
    public TransactionResponse deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody MoneyOperationRequest request
    ) {
        return accountService.deposit(accountNumber, request);
    }

    @PostMapping("/{accountNumber}/withdrawals")
    @Operation(summary = "Efetua saque")
    public TransactionResponse withdraw(
            @PathVariable String accountNumber,
            @Valid @RequestBody MoneyOperationRequest request
    ) {
        return accountService.withdraw(accountNumber, request);
    }

    @GetMapping("/{accountNumber}/statement")
    @Operation(summary = "Emite extrato por período")
    public StatementResponse statement(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return accountService.statement(accountNumber, startDate, endDate, page, size);
    }
}

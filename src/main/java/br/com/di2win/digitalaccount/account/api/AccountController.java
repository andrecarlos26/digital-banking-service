package br.com.di2win.digitalaccount.account.api;

import br.com.di2win.digitalaccount.account.api.dto.AccountResponse;
import br.com.di2win.digitalaccount.account.api.dto.BalanceResponse;
import br.com.di2win.digitalaccount.account.api.dto.CreateAccountRequest;
import br.com.di2win.digitalaccount.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{number}")
                .buildAndExpand(response.number())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse find(@PathVariable String accountNumber) {
        return accountService.find(accountNumber);
    }

    @GetMapping("/{accountNumber}/balance")
    public BalanceResponse balance(@PathVariable String accountNumber) {
        return accountService.balance(accountNumber);
    }

    @PatchMapping("/{accountNumber}/block")
    public AccountResponse block(@PathVariable String accountNumber) {
        return accountService.block(accountNumber);
    }

    @PatchMapping("/{accountNumber}/unblock")
    public AccountResponse unblock(@PathVariable String accountNumber) {
        return accountService.unblock(accountNumber);
    }
}

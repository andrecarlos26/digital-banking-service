package br.com.di2win.digitalaccount;

import br.com.di2win.digitalaccount.account.repository.AccountTransactionRepository;
import br.com.di2win.digitalaccount.account.repository.DigitalAccountRepository;
import br.com.di2win.digitalaccount.customer.repository.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountTransactionRepository transactionRepository;

    @Autowired
    private DigitalAccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void shouldDepositWithdrawAndReturnBalance() throws Exception {
        TestAccount testAccount = createAccount("529.982.247-25", "Cliente Financeiro");

        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", testAccount.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000.00,\"description\":\"Depósito inicial\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.balanceAfter").value(1000.00));

        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", testAccount.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":400.00,\"description\":\"Saque\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.balanceAfter").value(600.00));

        mockMvc.perform(get("/api/v1/accounts/{number}/balance", testAccount.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(600.00))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void shouldRejectOperationsWhileBlockedAndAllowAfterUnblock() throws Exception {
        TestAccount account = createAccount("111.444.777-35", "Cliente Bloqueio");

        mockMvc.perform(patch("/api/v1/accounts/{number}/block", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));

        mockMvc.perform(patch("/api/v1/accounts/{number}/unblock", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false));

        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldEnforceAccumulatedDailyWithdrawalLimit() throws Exception {
        TestAccount account = createAccount("935.411.347-80", "Cliente Limite");
        deposit(account.number(), "5000.00");

        withdraw(account.number(), "1500.00", 200);
        withdraw(account.number(), "500.00", 200);

        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0.01}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DAILY_WITHDRAWAL_LIMIT_EXCEEDED"));
    }

    @Test
    void shouldRejectInsufficientBalance() throws Exception {
        TestAccount account = createAccount("390.533.447-05", "Cliente Sem Saldo");

        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void shouldReturnStatementForPeriod() throws Exception {
        TestAccount account = createAccount("168.995.350-09", "Cliente Extrato");
        deposit(account.number(), "1000.00");
        withdraw(account.number(), "250.00", 200);
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        mockMvc.perform(get("/api/v1/accounts/{number}/statement", account.number())
                        .queryParam("startDate", today.toString())
                        .queryParam("endDate", today.toString())
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(0.00))
                .andExpect(jsonPath("$.totalDeposits").value(1000.00))
                .andExpect(jsonPath("$.totalWithdrawals").value(250.00))
                .andExpect(jsonPath("$.closingBalance").value(750.00))
                .andExpect(jsonPath("$.transactions.length()").value(2));
    }

    @Test
    void shouldPreventCustomerDeletionWithPositiveBalanceAndCloseWithZeroBalance() throws Exception {
        TestAccount account = createAccount("862.883.667-57", "Cliente Exclusão");
        deposit(account.number(), "100.00");

        mockMvc.perform(delete("/api/v1/customers/{id}", account.customerId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_HAS_POSITIVE_BALANCE"));

        withdraw(account.number(), "100.00", 200);

        mockMvc.perform(delete("/api/v1/customers/{id}", account.customerId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/accounts/{number}", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.blocked").value(true));
    }

    private TestAccount createAccount(String cpf, String name) throws Exception {
        String customerResponse = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","cpf":"%s","birthDate":"1990-01-10"}
                                """.formatted(name, cpf)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String customerId = objectMapper.readTree(customerResponse).get("id").asText();

        String accountResponse = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"%s\"}".formatted(cpf)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode account = objectMapper.readTree(accountResponse);
        return new TestAccount(customerId, account.get("number").asText());
    }

    private void deposit(String number, String amount) throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", number)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s}".formatted(amount)))
                .andExpect(status().isOk());
    }

    private void withdraw(String number, String amount, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", number)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s}".formatted(amount)))
                .andExpect(status().is(expectedStatus));
    }

    private record TestAccount(String customerId, String number) {
    }
}

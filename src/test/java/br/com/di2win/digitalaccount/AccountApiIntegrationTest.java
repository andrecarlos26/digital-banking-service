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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    private JdbcTemplate jdbcTemplate;

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
    void shouldCreateAccountAndReturnProvisioningData() throws Exception {
        String customerId = createCustomer("529.982.247-25", "Maria Silva");

        String response = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"529.982.247-25\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/accounts/\\d{8}")))
                .andExpect(jsonPath("$.number", matchesPattern("\\d{8}")))
                .andExpect(jsonPath("$.agency").value("0001"))
                .andExpect(jsonPath("$.balance").value(0.00))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.blocked").value(false))
                .andExpect(jsonPath("$.dailyWithdrawalLimit").value(2000.00))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.customerName").value("Maria Silva"))
                .andReturn().getResponse().getContentAsString();

        JsonNode account = objectMapper.readTree(response);
        String accountNumber = account.get("number").asText();

        mockMvc.perform(get("/api/v1/accounts/{number}", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(accountNumber));

        mockMvc.perform(get("/api/v1/accounts/{number}/balance", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.balance").value(0.00))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void shouldRejectSecondAccountForSameCustomer() throws Exception {
        createCustomer("111.444.777-35", "Cliente Único");
        String body = "{\"cpf\":\"111.444.777-35\"}";

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void shouldRejectInvalidOrUnknownCustomerCpf() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"111.111.111-11\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"935.411.347-80\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundForUnknownAccount() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{number}", "99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/accounts/{number}/balance", "99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void shouldBlockAndUnblockAccountIdempotently() throws Exception {
        TestAccount account = createAccount("935.411.347-80", "Cliente Bloqueio");

        mockMvc.perform(patch("/api/v1/accounts/{number}/block", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        mockMvc.perform(patch("/api/v1/accounts/{number}/block", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        mockMvc.perform(patch("/api/v1/accounts/{number}/unblock", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false));

        mockMvc.perform(patch("/api/v1/accounts/{number}/unblock", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false));
    }

    @Test
    void shouldCloseZeroBalanceAccountWhenCustomerIsDeleted() throws Exception {
        TestAccount account = createAccount("390.533.447-05", "Cliente Encerramento");

        mockMvc.perform(delete("/api/v1/customers/{id}", account.customerId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/customers/{id}", account.customerId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/accounts/{number}", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.blocked").value(true));

        mockMvc.perform(patch("/api/v1/accounts/{number}/unblock", account.number()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
    }

    @Test
    void shouldRejectCustomerDeletionWhenAccountHasPositiveBalance() throws Exception {
        TestAccount account = createAccount("168.995.350-09", "Cliente Com Saldo");
        jdbcTemplate.update("update accounts set balance = 100.00 where number = ?", account.number());

        mockMvc.perform(delete("/api/v1/customers/{id}", account.customerId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_HAS_POSITIVE_BALANCE"));

        mockMvc.perform(get("/api/v1/customers/{id}", account.customerId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts/{number}", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldDepositAndUpdateAccountBalance() throws Exception {
        TestAccount account = createAccount("862.883.667-57", "Cliente Depósito");

        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 1250.00,
                                  "description": "  Depósito   inicial  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.accountNumber").value(account.number()))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(1250.00))
                .andExpect(jsonPath("$.balanceAfter").value(1250.00))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.description").value("Depósito inicial"))
                .andExpect(jsonPath("$.occurredAt").exists());

        mockMvc.perform(get("/api/v1/accounts/{number}/balance", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1250.00));

        org.assertj.core.api.Assertions.assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldAccumulateMultipleDeposits() throws Exception {
        TestAccount account = createAccount("529.982.247-25", "Cliente Acúmulo");

        deposit(account.number(), "100.50");
        deposit(account.number(), "49.50");

        mockMvc.perform(get("/api/v1/accounts/{number}/balance", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));

        org.assertj.core.api.Assertions.assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldRejectDepositsForBlockedAndClosedAccounts() throws Exception {
        TestAccount blockedAccount = createAccount("153.509.460-56", "Cliente Bloqueado");

        mockMvc.perform(patch("/api/v1/accounts/{number}/block", blockedAccount.number()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", blockedAccount.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));

        TestAccount closedAccount = createAccount("280.012.389-38", "Cliente Encerrado");
        mockMvc.perform(delete("/api/v1/customers/{id}", closedAccount.customerId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", closedAccount.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
    }

    @Test
    void shouldValidateDepositRequest() throws Exception {
        TestAccount account = createAccount("111.444.777-35", "Cliente Validação");

        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        String longDescription = "a".repeat(121);
        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"description\":\"%s\"}".formatted(longDescription)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnNotFoundWhenDepositingIntoUnknownAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", "99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void shouldWithdrawAndUpdateAccountBalance() throws Exception {
        TestAccount account = createAccount("123.456.789-09", "Cliente Saque");
        deposit(account.number(), "1500.00");

        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 400.00,
                                  "description": "  Saque   em caixa  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.accountNumber").value(account.number()))
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(400.00))
                .andExpect(jsonPath("$.balanceAfter").value(1100.00))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.description").value("Saque em caixa"))
                .andExpect(jsonPath("$.occurredAt").exists());

        mockMvc.perform(get("/api/v1/accounts/{number}/balance", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1100.00));

        org.assertj.core.api.Assertions.assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldRejectWithdrawalWhenBalanceIsInsufficient() throws Exception {
        TestAccount account = createAccount("987.654.321-00", "Cliente Sem Saldo");
        deposit(account.number(), "100.00");

        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.01}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        mockMvc.perform(get("/api/v1/accounts/{number}/balance", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));

        org.assertj.core.api.Assertions.assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldEnforceAccumulatedDailyWithdrawalLimit() throws Exception {
        TestAccount account = createAccount("741.852.963-55", "Cliente Limite Diário");
        deposit(account.number(), "3000.00");

        withdraw(account.number(), "1500.00");
        withdraw(account.number(), "500.00");

        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", account.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0.01}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DAILY_WITHDRAWAL_LIMIT_EXCEEDED"));

        mockMvc.perform(get("/api/v1/accounts/{number}/balance", account.number()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.00));

        org.assertj.core.api.Assertions.assertThat(transactionRepository.count()).isEqualTo(3);
    }

    @Test
    void shouldRejectWithdrawalsForBlockedAndClosedAccounts() throws Exception {
        TestAccount blockedAccount = createAccount("321.654.987-91", "Cliente Saque Bloqueado");
        deposit(blockedAccount.number(), "100.00");

        mockMvc.perform(patch("/api/v1/accounts/{number}/block", blockedAccount.number()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", blockedAccount.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));

        TestAccount closedAccount = createAccount("456.789.123-64", "Cliente Saque Encerrado");
        mockMvc.perform(delete("/api/v1/customers/{id}", closedAccount.customerId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", closedAccount.number())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
    }

    @Test
    void shouldReturnNotFoundWhenWithdrawingFromUnknownAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", "99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    private void deposit(String accountNumber, String amount) throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{number}/deposits", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s}".formatted(amount)))
                .andExpect(status().isOk());
    }

    private void withdraw(String accountNumber, String amount) throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{number}/withdrawals", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s}".formatted(amount)))
                .andExpect(status().isOk());
    }

    private TestAccount createAccount(String cpf, String name) throws Exception {
        String customerId = createCustomer(cpf, name);
        String response = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"%s\"}".formatted(cpf)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new TestAccount(customerId, objectMapper.readTree(response).get("number").asText());
    }

    private String createCustomer(String cpf, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","cpf":"%s","birthDate":"1990-01-10"}
                                """.formatted(name, cpf)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private record TestAccount(String customerId, String number) {
    }
}

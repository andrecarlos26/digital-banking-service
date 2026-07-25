package br.com.di2win.digitalaccount;

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
    private DigitalAccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void cleanDatabase() {
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

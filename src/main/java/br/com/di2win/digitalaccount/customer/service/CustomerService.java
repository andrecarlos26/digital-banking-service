package br.com.di2win.digitalaccount.customer.service;

import br.com.di2win.digitalaccount.account.domain.DigitalAccount;
import br.com.di2win.digitalaccount.account.repository.DigitalAccountRepository;
import br.com.di2win.digitalaccount.common.exception.ApiException;
import br.com.di2win.digitalaccount.common.exception.ErrorCode;
import br.com.di2win.digitalaccount.customer.api.dto.CreateCustomerRequest;
import br.com.di2win.digitalaccount.customer.api.dto.CustomerResponse;
import br.com.di2win.digitalaccount.customer.domain.Customer;
import br.com.di2win.digitalaccount.customer.repository.CustomerRepository;
import br.com.di2win.digitalaccount.customer.validation.CpfUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final DigitalAccountRepository accountRepository;
    private final Clock clock;

    public CustomerService(
            CustomerRepository customerRepository,
            DigitalAccountRepository accountRepository,
            Clock clock
    ) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String cpf = CpfUtils.normalize(request.cpf());
        if (customerRepository.existsByCpf(cpf)) {
            throw cpfConflict();
        }

        Instant now = clock.instant();
        Customer customer = new Customer(
                UUID.randomUUID(),
                normalizeName(request.name()),
                cpf,
                request.birthDate(),
                now
        );

        try {
            return CustomerResponse.from(customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException exception) {
            throw cpfConflict();
        }
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        return CustomerResponse.from(findActiveById(id));
    }

    @Transactional
    public void delete(UUID id) {
        Customer customer = findActiveByIdForUpdate(id);
        Instant now = clock.instant();

        accountRepository.findByCustomerIdForUpdate(id).ifPresent(account -> closeAccount(account, now));
        customer.deactivate(now);
    }

    @Transactional(readOnly = true)
    public Customer findActiveByCpf(String rawCpf) {
        String cpf = CpfUtils.normalize(rawCpf);
        return customerRepository.findByCpfAndActiveTrue(cpf)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CUSTOMER_NOT_FOUND,
                        "Cliente não encontrado", "Não existe cliente ativo com o CPF informado."));
    }

    @Transactional
    public Customer findActiveByCpfForUpdate(String rawCpf) {
        String cpf = CpfUtils.normalize(rawCpf);
        return customerRepository.findActiveByCpfForUpdate(cpf)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CUSTOMER_NOT_FOUND,
                        "Cliente não encontrado", "Não existe cliente ativo com o CPF informado."));
    }

    private Customer findActiveById(UUID id) {
        return customerRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CUSTOMER_NOT_FOUND,
                        "Cliente não encontrado", "Não existe cliente ativo com o identificador informado."));
    }

    private Customer findActiveByIdForUpdate(UUID id) {
        return customerRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CUSTOMER_NOT_FOUND,
                        "Cliente não encontrado", "Não existe cliente ativo com o identificador informado."));
    }

    private void closeAccount(DigitalAccount account, Instant now) {
        if (account.getBalance().signum() > 0) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.CUSTOMER_HAS_POSITIVE_BALANCE,
                    "Conta possui saldo", "O cliente não pode ser removido enquanto a conta possuir saldo positivo.");
        }
        account.close(now);
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private ApiException cpfConflict() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.CPF_ALREADY_REGISTERED,
                "CPF já cadastrado", "Já existe um cliente com o CPF informado.");
    }
}

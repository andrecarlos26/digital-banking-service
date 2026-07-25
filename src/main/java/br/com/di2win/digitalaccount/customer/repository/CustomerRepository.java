package br.com.di2win.digitalaccount.customer.repository;

import br.com.di2win.digitalaccount.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByCpf(String cpf);

    Optional<Customer> findByCpfAndActiveTrue(String cpf);

    Optional<Customer> findByIdAndActiveTrue(UUID id);
}

package br.com.di2win.digitalaccount.customer.repository;

import br.com.di2win.digitalaccount.customer.domain.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByCpf(String cpf);

    Optional<Customer> findByCpfAndActiveTrue(String cpf);

    Optional<Customer> findByIdAndActiveTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id and c.active = true")
    Optional<Customer> findActiveByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.cpf = :cpf and c.active = true")
    Optional<Customer> findActiveByCpfForUpdate(@Param("cpf") String cpf);
}

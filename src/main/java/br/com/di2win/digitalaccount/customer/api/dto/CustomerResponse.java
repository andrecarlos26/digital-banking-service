package br.com.di2win.digitalaccount.customer.api.dto;

import br.com.di2win.digitalaccount.customer.domain.Customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String cpf,
        LocalDate birthDate,
        Instant createdAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                formatCpf(customer.getCpf()),
                customer.getBirthDate(),
                customer.getCreatedAt()
        );
    }

    private static String formatCpf(String cpf) {
        return cpf.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }
}

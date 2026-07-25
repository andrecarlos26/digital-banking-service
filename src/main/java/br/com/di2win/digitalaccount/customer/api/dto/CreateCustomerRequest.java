package br.com.di2win.digitalaccount.customer.api.dto;

import br.com.di2win.digitalaccount.customer.validation.ValidCpf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCustomerRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 150, message = "Nome deve possuir entre 2 e 150 caracteres")
        String name,

        @NotBlank(message = "CPF é obrigatório")
        @ValidCpf
        String cpf,

        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "Data de nascimento deve estar no passado")
        LocalDate birthDate
) {
}

package br.com.di2win.digitalaccount.account.api.dto;

import br.com.di2win.digitalaccount.customer.validation.ValidCpf;
import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(
        @NotBlank(message = "CPF é obrigatório")
        @ValidCpf
        String cpf
) {
}

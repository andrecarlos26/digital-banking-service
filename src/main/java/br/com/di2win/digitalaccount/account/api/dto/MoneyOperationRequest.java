package br.com.di2win.digitalaccount.account.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MoneyOperationRequest(
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior ou igual a 0,01")
        @Digits(integer = 17, fraction = 2, message = "Valor deve possuir no máximo duas casas decimais")
        BigDecimal amount,

        @Size(max = 120, message = "Descrição deve possuir no máximo 120 caracteres")
        String description
) {
}

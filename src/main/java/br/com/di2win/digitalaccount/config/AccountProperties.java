package br.com.di2win.digitalaccount.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.ZoneId;

@Validated
@ConfigurationProperties(prefix = "di2win.account")
public record AccountProperties(
        @NotBlank String agency,
        @NotNull @DecimalMin("0.01") BigDecimal dailyWithdrawalLimit,
        @NotNull ZoneId businessTimezone
) {
}

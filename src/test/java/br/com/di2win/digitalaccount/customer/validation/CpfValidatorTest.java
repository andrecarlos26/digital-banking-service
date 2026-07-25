package br.com.di2win.digitalaccount.customer.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @Test
    void shouldAcceptValidFormattedAndUnformattedCpf() {
        assertThat(validator.isValid("529.982.247-25", null)).isTrue();
        assertThat(validator.isValid("11144477735", null)).isTrue();
    }

    @Test
    void shouldRejectInvalidCpf() {
        assertThat(validator.isValid("111.111.111-11", null)).isFalse();
        assertThat(validator.isValid("529.982.247-24", null)).isFalse();
        assertThat(validator.isValid("123", null)).isFalse();
        assertThat(validator.isValid(null, null)).isFalse();
    }
}

package br.com.di2win.digitalaccount.customer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String cpf = CpfUtils.normalize(value);
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }

        return calculateDigit(cpf, 9) == Character.getNumericValue(cpf.charAt(9))
                && calculateDigit(cpf, 10) == Character.getNumericValue(cpf.charAt(10));
    }

    private int calculateDigit(String cpf, int length) {
        int sum = 0;
        int weight = length + 1;
        for (int index = 0; index < length; index++) {
            sum += Character.getNumericValue(cpf.charAt(index)) * weight--;
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}

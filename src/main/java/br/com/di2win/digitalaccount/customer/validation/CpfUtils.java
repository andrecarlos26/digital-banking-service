package br.com.di2win.digitalaccount.customer.validation;

public final class CpfUtils {

    private CpfUtils() {
    }

    public static String normalize(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }
}

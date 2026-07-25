package br.com.di2win.digitalaccount;

import br.com.di2win.digitalaccount.config.AccountProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AccountProperties.class)
public class DigitalAccountApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalAccountApplication.class, args);
    }
}

package br.com.di2win.digitalaccount.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Bean
    Clock businessClock(AccountProperties properties) {
        return Clock.system(properties.businessTimezone());
    }
}

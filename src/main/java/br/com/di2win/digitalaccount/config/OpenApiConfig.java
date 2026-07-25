package br.com.di2win.digitalaccount.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI digitalAccountOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Di2win Digital Account API")
                .description("API para clientes, contas digitais, depósitos, saques, bloqueios e extratos.")
                .version("1.0.0")
                .contact(new Contact().name("Di2win Backend Challenge")));
    }
}

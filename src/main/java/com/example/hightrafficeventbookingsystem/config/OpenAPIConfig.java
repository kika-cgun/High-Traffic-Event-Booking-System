package com.example.hightrafficeventbookingsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("High Traffic Event Booking System API")
                        .version("1.0")
                        .description("API documentation for the High Traffic Event Booking System")
                        .contact(new Contact()
                                .name("Piotr Capecki")
                                .email("kontakt@piotr-capecki.pl")
                                .url("piotr-capecki.pl")
                        )
        );
    }
}

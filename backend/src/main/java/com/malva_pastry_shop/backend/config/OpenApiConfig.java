package com.malva_pastry_shop.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuración de OpenAPI/Swagger para documentación de la API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Malva Pastry Shop API")
                        .version("1.0.0")
                        .description("API pública para consultar productos, categorías y tags de la pastelería")
                        .contact(new Contact()
                                .name("Malva Pastry Shop")
                                .email("contacto@malva.com")))
                .addServersItem(new Server()
                        .url("/")
                        .description("Servidor local"));
    }
}

package com.fittracker.user.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos OpenAPI/Swagger del servicio de usuarios y perfiles.
 * La validacion JWT real la realiza el api-gateway; el esquema documenta el uso de Bearer.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI fitTrackerUserOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FitTracker User API")
                        .description("Perfiles de usuario fisicos y objetivos. El perfil se crea de forma asincrona "
                                + "al registrarse (evento user.created via RabbitMQ). "
                                + "El api-gateway (:8222) valida el JWT y propaga X-User-Id/X-User-Email/X-User-Roles.")
                        .version("1.0.0")
                        .contact(new Contact().name("FitTracker").url("https://github.com/kevinshz/FitTracker")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

package com.ejr.managekitchen.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Documentation Swagger : http://localhost:8080/swagger-ui.html (bouton « Authorize » pour le JWT). */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "EJR-ManageKitchen API",
                version = "0.1.0",
                description = "Plateforme SaaS de restauration connectée (B2B & B2C) — Master 1 MIAGE"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}

package br.com.lettergames.catalogo.api;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Definição OpenAPI e esquema de autenticação HTTP Basic do {@code catalogo-service}.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "LetterGames — Catálogo Service API",
                version = "1.0.0",
                description = "API REST para gerenciamento e consulta do catálogo de jogos do LetterGames"
        )
)
@SecurityScheme(
        securitySchemeName = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Autenticação HTTP Basic validada contra usuários em banco com hash BCrypt"
)
public class CatalogoOpenApiConfig extends Application {
}

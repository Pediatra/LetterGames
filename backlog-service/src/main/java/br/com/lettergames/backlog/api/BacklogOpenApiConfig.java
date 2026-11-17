package br.com.lettergames.backlog.api;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Definição OpenAPI e esquema HTTP Basic do {@code backlog-service}.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "LetterGames — Backlog Service API",
                version = "1.0.0",
                description = "API REST para gerenciamento de backlog de jogos, integração resiliente com o catálogo e resumo do jogador"
        )
)
@SecurityScheme(
        securitySchemeName = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Autenticação HTTP Basic validada contra usuários em banco com hash BCrypt"
)
public class BacklogOpenApiConfig extends Application {
}

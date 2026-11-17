package br.com.lettergames.backlog.integracao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Filtro REST Client que envia credenciais HTTP Basic de serviço para o {@code catalogo-service}.
 */
@ApplicationScoped
public class CatalogoBasicAuthFilter implements ClientRequestFilter {

    private final String authorizationHeader;

    @Inject
    public CatalogoBasicAuthFilter(
            @ConfigProperty(name = "catalogo.auth.usuario") String usuario,
            @ConfigProperty(name = "catalogo.auth.senha") String senha) {
        String credenciais = usuario + ":" + senha;
        String token = Base64.getEncoder().encodeToString(credenciais.getBytes(StandardCharsets.UTF_8));
        this.authorizationHeader = "Basic " + token;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, authorizationHeader);
    }
}

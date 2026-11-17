package br.com.lettergames.backlog.integracao;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente HTTP declarativo para consumo síncrono do {@code catalogo-service}.
 */
@Path("/jogos")
@RegisterRestClient(configKey = "catalogo-api")
@RegisterProvider(CatalogoBasicAuthFilter.class)
@RegisterProvider(CatalogoClientExceptionMapper.class)
public interface CatalogoRestClient {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    JogoCatalogoDTO buscarPorId(@PathParam("id") Long id);
}

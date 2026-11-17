package br.com.lettergames.catalogo.api.erros;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Fallback global para exceções inesperadas (HTTP 500), ocultando stack trace e detalhes de banco.
 */
@Provider
@Priority(1)
public class ErroInesperadoExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ErroInesperadoExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("Erro inesperado ao processar requisição no catálogo", exception);
        return ErroPadraoResponseBuilder.construir(
                500,
                "ERRO_INTERNO",
                "Erro interno ao processar a requisição",
                uriInfo
        );
    }
}

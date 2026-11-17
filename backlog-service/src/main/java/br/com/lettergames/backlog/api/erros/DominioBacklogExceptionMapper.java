package br.com.lettergames.backlog.api.erros;

import br.com.lettergames.backlog.dominio.DominioBacklogException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Mapeia exceções de domínio do backlog para o corpo único {@link br.com.lettergames.backlog.api.ErroPadrao}.
 */
@Provider
@Priority(1)
public class DominioBacklogExceptionMapper implements ExceptionMapper<DominioBacklogException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(DominioBacklogException exception) {
        return ErroPadraoResponseBuilder.construir(
                exception.getStatus(),
                exception.getCodigo(),
                exception.getMessage(),
                uriInfo
        );
    }
}

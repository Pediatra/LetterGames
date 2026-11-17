package br.com.lettergames.catalogo.api.erros;

import br.com.lettergames.catalogo.dominio.DominioCatalogoException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Mapeia exceções de domínio do catálogo para o corpo único {@link br.com.lettergames.catalogo.api.ErroPadrao}.
 */
@Provider
@Priority(1)
public class DominioCatalogoExceptionMapper implements ExceptionMapper<DominioCatalogoException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(DominioCatalogoException exception) {
        return ErroPadraoResponseBuilder.construir(
                exception.getStatus(),
                exception.getCodigo(),
                exception.getMessage(),
                uriInfo
        );
    }
}

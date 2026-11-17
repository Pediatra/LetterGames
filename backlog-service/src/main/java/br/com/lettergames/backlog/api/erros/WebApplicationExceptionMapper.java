package br.com.lettergames.backlog.api.erros;

import com.fasterxml.jackson.core.JacksonException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Padroniza respostas de protocolo JAX-RS (400, 404, 405, 415) sem expor detalhes internos.
 */
@Provider
@Priority(1)
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        if (exception.getCause() instanceof JacksonException) {
            return ErroPadraoResponseBuilder.construir(
                    400,
                    "JSON_INVALIDO",
                    "Corpo JSON malformado ou com tipos de dados incompatíveis",
                    uriInfo
            );
        }
        int status = exception.getResponse() != null ? exception.getResponse().getStatus() : 500;
        return switch (status) {
            case 400 -> ErroPadraoResponseBuilder.construir(400, "REQUISICAO_INVALIDA", "Requisição inválida", uriInfo);
            case 404 -> ErroPadraoResponseBuilder.construir(404, "RECURSO_NAO_ENCONTRADO", "Recurso não encontrado", uriInfo);
            case 405 -> ErroPadraoResponseBuilder.construir(405, "METODO_NAO_PERMITIDO", "Método HTTP não permitido para este recurso", uriInfo);
            case 415 -> ErroPadraoResponseBuilder.construir(415, "MIDIA_NAO_SUPORTADA", "Content-Type não suportado; utilize application/json", uriInfo);
            default -> ErroPadraoResponseBuilder.construir(status, "ERRO_HTTP_" + status, "Não foi possível processar a requisição", uriInfo);
        };
    }
}

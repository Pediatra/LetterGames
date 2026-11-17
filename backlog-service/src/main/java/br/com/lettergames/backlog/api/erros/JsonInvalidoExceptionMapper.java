package br.com.lettergames.backlog.api.erros;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Mapeia JSON malformado ou valores incompatíveis (como enum inválido) para HTTP 400 com {@code ErroPadrao}.
 */
public class JsonInvalidoExceptionMapper {

    @ServerExceptionMapper(priority = 1)
    public Response jacksonException(JacksonException exception, UriInfo uriInfo) {
        return respostaJsonInvalido(uriInfo);
    }

    @ServerExceptionMapper(priority = 1)
    public Response mismatchedInput(MismatchedInputException exception, UriInfo uriInfo) {
        return respostaJsonInvalido(uriInfo);
    }

    private static Response respostaJsonInvalido(UriInfo uriInfo) {
        return ErroPadraoResponseBuilder.construir(
                400,
                "JSON_INVALIDO",
                "Corpo JSON malformado ou com tipos de dados incompatíveis",
                uriInfo
        );
    }
}

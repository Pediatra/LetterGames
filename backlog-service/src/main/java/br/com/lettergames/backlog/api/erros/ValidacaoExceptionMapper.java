package br.com.lettergames.backlog.api.erros;

import br.com.lettergames.backlog.api.ErroPadrao.CampoErro;
import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Comparator;
import java.util.List;

/**
 * Mapeia violações de Bean Validation para HTTP 400 com lista ordenada de campos inválidos.
 */
@Provider
@Priority(1)
public class ValidacaoExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<CampoErro> detalhes = exception.getConstraintViolations().stream()
                .map(ValidacaoExceptionMapper::paraCampoErro)
                .sorted(Comparator.comparing(CampoErro::campo).thenComparing(CampoErro::mensagem))
                .toList();
        return ErroPadraoResponseBuilder.construirComDetalhes(
                400,
                "ERRO_VALIDACAO",
                "Dados da requisição inválidos",
                uriInfo,
                detalhes
        );
    }

    private static CampoErro paraCampoErro(ConstraintViolation<?> violacao) {
        String caminho = violacao.getPropertyPath() == null ? "" : violacao.getPropertyPath().toString();
        int ultimoPonto = caminho.lastIndexOf('.');
        String campo = ultimoPonto >= 0 ? caminho.substring(ultimoPonto + 1) : caminho;
        if (campo.isBlank()) {
            campo = "requisicao";
        }
        return new CampoErro(campo, violacao.getMessage());
    }
}

package br.com.lettergames.catalogo.api.erros;

import br.com.lettergames.catalogo.api.ErroPadrao;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Mapeia exceções de autenticação (401) e autorização (403) para o corpo {@link ErroPadrao}.
 */
public class SegurancaExceptionMapper {

    @ServerExceptionMapper(priority = 1)
    public Response naoAutenticado(UnauthorizedException exception, UriInfo uriInfo) {
        return resposta401(uriInfo);
    }

    @ServerExceptionMapper(priority = 1)
    public Response falhaAutenticacao(AuthenticationFailedException exception, UriInfo uriInfo) {
        return resposta401(uriInfo);
    }

    @ServerExceptionMapper(priority = 1)
    public Response acessoNegado(ForbiddenException exception, UriInfo uriInfo) {
        return ErroPadraoResponseBuilder.construir(
                403,
                "ACESSO_NEGADO",
                "Usuário autenticado não possui permissão para esta operação",
                uriInfo
        );
    }

    private static Response resposta401(UriInfo uriInfo) {
        ErroPadrao body = ErroPadrao.de(
                401,
                "NAO_AUTENTICADO",
                "Autenticação HTTP Basic obrigatória ou credenciais inválidas",
                ErroPadraoResponseBuilder.caminho(uriInfo)
        );
        return Response.status(401)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"lettergames\"")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(body)
                .build();
    }
}

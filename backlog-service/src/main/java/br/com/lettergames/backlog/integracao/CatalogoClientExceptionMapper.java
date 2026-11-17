package br.com.lettergames.backlog.integracao;

import br.com.lettergames.backlog.dominio.CatalogoRequisicaoInvalidaException;
import br.com.lettergames.backlog.dominio.JogoInexistenteNoCatalogoException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Traduz códigos HTTP de erro do {@code catalogo-service} para exceções específicas do backlog.
 */
public class CatalogoClientExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    @Override
    public RuntimeException toThrowable(Response response) {
        int status = response.getStatus();
        if (status == 404) {
            return new JogoInexistenteNoCatalogoException();
        }
        if (status >= 400 && status < 500) {
            return new CatalogoRequisicaoInvalidaException(status);
        }
        return new CatalogoRemotoException("Catálogo respondeu com status HTTP " + status);
    }
}

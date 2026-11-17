package br.com.lettergames.backlog.integracao;

/**
 * Erro transitório ou de resposta 5xx retornado pelo {@code catalogo-service}, elegível a retry e circuit breaker.
 */
public class CatalogoRemotoException extends RuntimeException {

    public CatalogoRemotoException(String mensagem) {
        super(mensagem);
    }
}

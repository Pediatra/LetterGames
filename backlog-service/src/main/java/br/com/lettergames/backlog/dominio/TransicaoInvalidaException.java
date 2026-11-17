package br.com.lettergames.backlog.dominio;

/**
 * Lançada quando a mudança de status viola as transições permitidas (HTTP 422).
 */
public class TransicaoInvalidaException extends DominioBacklogException {

    public TransicaoInvalidaException(String mensagem) {
        super(422, "TRANSICAO_INVALIDA", mensagem);
    }
}

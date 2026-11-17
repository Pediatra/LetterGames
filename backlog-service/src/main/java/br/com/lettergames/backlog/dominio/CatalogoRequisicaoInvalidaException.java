package br.com.lettergames.backlog.dominio;

/**
 * Lançada quando o {@code catalogo-service} recusa a requisição com erro 4xx diferente de 404 (HTTP 502).
 */
public class CatalogoRequisicaoInvalidaException extends DominioBacklogException {

    public CatalogoRequisicaoInvalidaException(int statusRemoto) {
        super(502, "ERRO_INTEGRACAO_CATALOGO",
                "Erro de integração ao consultar o catálogo (status remoto " + statusRemoto + ")");
    }
}

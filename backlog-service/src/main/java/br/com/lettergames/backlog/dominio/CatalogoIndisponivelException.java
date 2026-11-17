package br.com.lettergames.backlog.dominio;

/**
 * Lançada quando o catálogo está indisponível ou com circuito aberto e o fallback bloqueia a escrita (HTTP 503).
 */
public class CatalogoIndisponivelException extends DominioBacklogException {

    public CatalogoIndisponivelException(Throwable causa) {
        super(503, "CATALOGO_INDISPONIVEL",
                "Catálogo indisponível no momento, tente novamente em instantes", causa);
    }
}

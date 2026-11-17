package br.com.lettergames.backlog.dominio;

/**
 * Lançada quando o jogo consultado não existe no catálogo remoto (HTTP 422).
 */
public class JogoInexistenteNoCatalogoException extends DominioBacklogException {

    public JogoInexistenteNoCatalogoException() {
        super(422, "JOGO_INEXISTENTE", "Jogo solicitado não existe no catálogo");
    }

    public JogoInexistenteNoCatalogoException(Long jogoId) {
        super(422, "JOGO_INEXISTENTE", "Jogo " + jogoId + " não existe no catálogo");
    }
}

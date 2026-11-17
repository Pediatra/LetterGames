package br.com.lettergames.catalogo.dominio;

/**
 * Lançada quando o jogo solicitado não existe no catálogo (HTTP 404).
 */
public class JogoNaoEncontradoException extends DominioCatalogoException {

    public JogoNaoEncontradoException(Long id) {
        super(404, "JOGO_NAO_ENCONTRADO", "Jogo " + id + " não encontrado no catálogo");
    }
}

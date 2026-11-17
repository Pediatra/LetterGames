package br.com.lettergames.catalogo.dominio;

/**
 * Lançada quando já existe jogo cadastrado com o mesmo título e plataforma (HTTP 409).
 */
public class JogoDuplicadoException extends DominioCatalogoException {

    public JogoDuplicadoException(String titulo, String plataforma) {
        super(409, "JOGO_DUPLICADO",
                "Já existe o jogo '" + titulo + "' cadastrado para a plataforma '" + plataforma + "'");
    }
}

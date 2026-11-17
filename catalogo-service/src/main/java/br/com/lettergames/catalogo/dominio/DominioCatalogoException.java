package br.com.lettergames.catalogo.dominio;

/**
 * Exceção base de domínio do catálogo, carregando o status HTTP e código funcional padronizado.
 */
public abstract class DominioCatalogoException extends RuntimeException {

    private final int status;
    private final String codigo;

    protected DominioCatalogoException(int status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public int getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}

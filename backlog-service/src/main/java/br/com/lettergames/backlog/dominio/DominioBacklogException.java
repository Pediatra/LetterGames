package br.com.lettergames.backlog.dominio;

/**
 * Exceção base de domínio do backlog, carregando status HTTP e código funcional padronizado.
 */
public abstract class DominioBacklogException extends RuntimeException {

    private final int status;
    private final String codigo;

    protected DominioBacklogException(int status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    protected DominioBacklogException(int status, String codigo, String mensagem, Throwable causa) {
        super(mensagem, causa);
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

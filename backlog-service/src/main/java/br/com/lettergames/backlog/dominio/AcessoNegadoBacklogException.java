package br.com.lettergames.backlog.dominio;

/**
 * Lançada quando o jogador tenta acessar ou alterar o backlog de outro usuário (HTTP 403).
 */
public class AcessoNegadoBacklogException extends DominioBacklogException {

    public AcessoNegadoBacklogException(String usuarioSolicitado) {
        super(403, "ACESSO_NEGADO",
                "Você não tem permissão para acessar ou alterar o backlog do usuário '" + usuarioSolicitado + "'");
    }
}

package br.com.lettergames.backlog.dominio;

/**
 * Lançada quando o jogador tenta adicionar um jogo já presente no próprio backlog (HTTP 409).
 */
public class JogoJaNoBacklogException extends DominioBacklogException {

    public JogoJaNoBacklogException(String usuarioId, Long jogoId) {
        super(409, "JOGO_JA_NO_BACKLOG",
                "O jogo " + jogoId + " já está no backlog de '" + usuarioId + "'");
    }
}

package br.com.lettergames.backlog.dominio;

/**
 * Lançada quando o jogador atinge o máximo de jogos simultâneos com status {@code JOGANDO} (HTTP 422).
 */
public class LimiteJogandoAtingidoException extends DominioBacklogException {

    public LimiteJogandoAtingidoException(String usuarioId, int limite) {
        super(422, "LIMITE_JOGANDO_ATINGIDO",
                "Usuário '" + usuarioId + "' já atingiu o limite de " + limite + " jogos com status JOGANDO");
    }
}

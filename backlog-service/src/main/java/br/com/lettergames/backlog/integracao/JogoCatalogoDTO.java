package br.com.lettergames.backlog.integracao;

/**
 * Representação do jogo recebida do {@code catalogo-service} via REST Client.
 */
public record JogoCatalogoDTO(
        Long id,
        String titulo,
        String plataforma,
        String genero,
        Integer duracaoMediaHoras) {
}

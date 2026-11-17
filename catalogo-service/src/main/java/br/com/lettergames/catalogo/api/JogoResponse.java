package br.com.lettergames.catalogo.api;

import br.com.lettergames.catalogo.dominio.Jogo;
import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Representação pública de um jogo retornado pelo {@code catalogo-service}.
 */
@Schema(name = "JogoResponse", description = "Representação pública de um jogo do catálogo")
public record JogoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Hollow Knight") String titulo,
        @Schema(example = "PC") String plataforma,
        @Schema(example = "Metroidvania") String genero,
        @Schema(example = "27") Integer duracaoMediaHoras,
        Instant atualizadoEm) {

    public static JogoResponse de(Jogo jogo) {
        return new JogoResponse(
                jogo.getId(),
                jogo.getTitulo(),
                jogo.getPlataforma(),
                jogo.getGenero(),
                jogo.getDuracaoMediaHoras(),
                jogo.getAtualizadoEm()
        );
    }
}

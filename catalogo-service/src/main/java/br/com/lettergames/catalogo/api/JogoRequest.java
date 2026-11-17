package br.com.lettergames.catalogo.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Corpo de entrada para criação e atualização de jogos em {@code /jogos}.
 */
@Schema(name = "JogoRequest", description = "Dados para cadastro ou atualização de um jogo no catálogo")
public record JogoRequest(
        @Schema(description = "Título do jogo", example = "Hollow Knight")
        @NotBlank(message = "título é obrigatório")
        @Size(max = 200, message = "título deve ter no máximo 200 caracteres")
        @Pattern(regexp = SEM_CONTROLE, message = "título não pode conter caracteres de controle")
        String titulo,

        @Schema(description = "Plataforma principal", example = "PC")
        @NotBlank(message = "plataforma é obrigatória")
        @Size(max = 50, message = "plataforma deve ter no máximo 50 caracteres")
        @Pattern(regexp = SEM_CONTROLE, message = "plataforma não pode conter caracteres de controle")
        String plataforma,

        @Schema(description = "Gênero do jogo", example = "Metroidvania")
        @NotBlank(message = "gênero é obrigatório")
        @Size(max = 50, message = "gênero deve ter no máximo 50 caracteres")
        @Pattern(regexp = SEM_CONTROLE, message = "gênero não pode conter caracteres de controle")
        String genero,

        @Schema(description = "Duração média estimada em horas", example = "27")
        @NotNull(message = "duração média em horas é obrigatória")
        @Positive(message = "duração média em horas deve ser maior que zero")
        Integer duracaoMediaHoras) {

    /** Rejeita controles ASCII/Unicode (inclui \n, \r, \t, NEL) e separadores de linha U+2028/U+2029. */
    static final String SEM_CONTROLE = "[^\\p{Cc}\\u2028\\u2029]*";
}

package br.com.lettergames.backlog.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Corpo de entrada para adicionar um jogo do catálogo ao backlog de um jogador.
 */
@Schema(name = "AdicionarItemRequest", description = "Dados para inclusão de jogo no backlog")
public record AdicionarItemRequest(
        @Schema(description = "Identificador do jogo no catalogo-service", example = "1")
        @NotNull(message = "jogoId é obrigatório")
        @Positive(message = "jogoId deve ser maior que zero")
        Long jogoId) {
}

package br.com.lettergames.backlog.api;

import br.com.lettergames.backlog.dominio.StatusBacklog;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Corpo de entrada para alteração de status de um item do backlog.
 */
@Schema(name = "AlterarStatusRequest", description = "Novo status desejado para o item no backlog")
public record AlterarStatusRequest(
        @Schema(description = "Novo status", example = "JOGANDO")
        @NotNull(message = "status é obrigatório")
        StatusBacklog status) {
}

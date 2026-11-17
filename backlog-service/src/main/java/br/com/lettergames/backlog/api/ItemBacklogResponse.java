package br.com.lettergames.backlog.api;

import br.com.lettergames.backlog.dominio.ItemBacklog;
import br.com.lettergames.backlog.dominio.StatusBacklog;
import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Representação pública de um item do backlog.
 */
@Schema(name = "ItemBacklogResponse", description = "Item salvo no backlog do jogador")
public record ItemBacklogResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "joao") String usuarioId,
        @Schema(example = "1") Long jogoId,
        @Schema(example = "Hollow Knight") String titulo,
        @Schema(example = "27") Integer duracaoMediaHoras,
        @Schema(example = "QUERO_JOGAR") StatusBacklog status,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static ItemBacklogResponse de(ItemBacklog item) {
        return new ItemBacklogResponse(
                item.getId(),
                item.getUsuarioId(),
                item.getJogoId(),
                item.getTitulo(),
                item.getDuracaoMediaHoras(),
                item.getStatus(),
                item.getCriadoEm(),
                item.getAtualizadoEm()
        );
    }
}

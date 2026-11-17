package br.com.lettergames.backlog.api;

import br.com.lettergames.backlog.dominio.StatusBacklog;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Resumo consolidado do backlog de um jogador, calculado por agregação Panache/JPQL.
 */
@Schema(name = "ResumoBacklogResponse", description = "Contagens por status, horas pendentes, horas zeradas e percentual concluído")
public record ResumoBacklogResponse(
        @Schema(example = "joao") String usuarioId,
        @Schema(example = "4") int total,
        Map<StatusBacklog, Integer> porStatus,
        @Schema(example = "70") int horasPendentes,
        @Schema(example = "27") int horasZeradas,
        @Schema(example = "25.0") double percentualZerado) {
}

package br.com.lettergames.backlog.api;

import br.com.lettergames.backlog.dominio.StatusBacklog;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Visão consolidada global de todos os backlogs para consulta administrativa.
 */
@Schema(name = "EstatisticasGlobaisResponse", description = "Estatísticas agregadas de todos os jogadores")
public record EstatisticasGlobaisResponse(
        long totalJogadoresComBacklog,
        int totalItens,
        Map<StatusBacklog, Integer> porStatus,
        int horasPendentes,
        int horasZeradas,
        double percentualZerado) {
}

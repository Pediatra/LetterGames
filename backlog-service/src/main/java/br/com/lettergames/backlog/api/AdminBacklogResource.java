package br.com.lettergames.backlog.api;

import br.com.lettergames.backlog.dominio.BacklogService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Recurso administrativo para estatísticas consolidadas entre todos os jogadores.
 */
@Path("/admin/backlogs")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Administração de Backlogs", description = "Consultas gerenciais consolidadas (papel ADMIN)")
@SecurityRequirement(name = "basicAuth")
public class AdminBacklogResource {

    private final BacklogService backlogService;

    @Inject
    public AdminBacklogResource(BacklogService backlogService) {
        this.backlogService = backlogService;
    }

    @GET
    @Path("/estatisticas")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Estatísticas globais", description = "Agrega totais e horas de todos os backlogs (requer papel ADMIN)")
    @APIResponse(responseCode = "200", description = "Estatísticas agregadas", content = @Content(schema = @Schema(implementation = EstatisticasGlobaisResponse.class)))
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Acesso restrito a ADMIN", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public EstatisticasGlobaisResponse estatisticas() {
        return backlogService.estatisticasGlobais();
    }
}

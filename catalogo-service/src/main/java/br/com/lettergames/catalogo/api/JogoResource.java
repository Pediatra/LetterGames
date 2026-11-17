package br.com.lettergames.catalogo.api;

import br.com.lettergames.catalogo.dominio.JogoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Recurso REST para cadastro, consulta, atualização e remoção de jogos do catálogo.
 */
@Path("/jogos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Catálogo de Jogos", description = "Operações CRUD e filtros sobre o catálogo de jogos")
@SecurityRequirement(name = "basicAuth")
public class JogoResource {

    private final JogoService jogoService;

    @Inject
    public JogoResource(JogoService jogoService) {
        this.jogoService = jogoService;
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Cadastrar jogo", description = "Cadastra um novo jogo no catálogo (requer papel ADMIN)")
    @APIResponse(
            responseCode = "201",
            description = "Jogo criado com sucesso",
            headers = @Header(name = "Location", description = "URI do jogo criado", schema = @Schema(type = SchemaType.STRING)),
            content = @Content(schema = @Schema(implementation = JogoResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Erro de validação ou JSON inválido", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Acesso restrito a ADMIN", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "409", description = "Jogo já cadastrado para a plataforma", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "422", description = "Duração acima do limite configurado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public Response criar(
            @Valid @NotNull(message = "corpo da requisição é obrigatório") JogoRequest requisicao,
            @Context UriInfo uriInfo) {
        JogoResponse criado = jogoService.criar(requisicao);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.id())).build();
        return Response.created(location).entity(criado).build();
    }

    @GET
    @RolesAllowed({"JOGADOR", "ADMIN"})
    @Operation(summary = "Listar jogos", description = "Lista jogos com filtros opcionais por plataforma, gênero e trecho do título")
    @APIResponse(
            responseCode = "200",
            description = "Lista de jogos ordenada por título",
            content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = JogoResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public List<JogoResponse> listar(
            @Parameter(description = "Filtrar por plataforma exata (case-insensitive)") @QueryParam("plataforma") String plataforma,
            @Parameter(description = "Filtrar por gênero exato (case-insensitive)") @QueryParam("genero") String genero,
            @Parameter(description = "Filtrar por trecho do título (case-insensitive)") @QueryParam("termo") String termo) {
        return jogoService.listar(plataforma, genero, termo);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"JOGADOR", "ADMIN"})
    @Operation(summary = "Buscar jogo por ID", description = "Retorna os detalhes de um jogo específico do catálogo")
    @APIResponse(responseCode = "200", description = "Jogo encontrado", content = @Content(schema = @Schema(implementation = JogoResponse.class)))
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "404", description = "Jogo não encontrado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public JogoResponse buscarPorId(@PathParam("id") Long id) {
        return jogoService.buscarPorId(id);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Atualizar jogo", description = "Atualiza os dados de um jogo existente (requer papel ADMIN)")
    @APIResponse(responseCode = "200", description = "Jogo atualizado", content = @Content(schema = @Schema(implementation = JogoResponse.class)))
    @APIResponse(responseCode = "400", description = "Erro de validação ou JSON inválido", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Acesso restrito a ADMIN", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "404", description = "Jogo não encontrado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "409", description = "Título e plataforma duplicados", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "422", description = "Duração acima do limite configurado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public JogoResponse atualizar(
            @PathParam("id") Long id,
            @Valid @NotNull(message = "corpo da requisição é obrigatório") JogoRequest requisicao) {
        return jogoService.atualizar(id, requisicao);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Remover jogo", description = "Remove um jogo do catálogo (requer papel ADMIN)")
    @APIResponse(responseCode = "204", description = "Jogo removido com sucesso")
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Acesso restrito a ADMIN", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "404", description = "Jogo não encontrado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public Response remover(@PathParam("id") Long id) {
        jogoService.remover(id);
        return Response.noContent().build();
    }
}

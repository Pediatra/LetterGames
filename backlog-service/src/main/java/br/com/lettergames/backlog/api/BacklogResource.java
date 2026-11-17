package br.com.lettergames.backlog.api;

import br.com.lettergames.backlog.dominio.BacklogService;
import br.com.lettergames.backlog.dominio.StatusBacklog;
import br.com.lettergames.backlog.seguranca.AutorizadorBacklog;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
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
 * Recurso REST para gerenciamento do backlog pessoal de cada jogador.
 */
@Path("/usuarios/{usuarioId}/backlog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Backlog do Jogador", description = "Operações sobre os jogos no backlog e estatísticas do jogador")
@SecurityRequirement(name = "basicAuth")
public class BacklogResource {

    private static final String PADRAO_USUARIO = "[A-Za-z0-9._-]{1,64}";

    private final BacklogService backlogService;
    private final AutorizadorBacklog autorizador;

    @Inject
    public BacklogResource(BacklogService backlogService, AutorizadorBacklog autorizador) {
        this.backlogService = backlogService;
        this.autorizador = autorizador;
    }

    @POST
    @RolesAllowed("JOGADOR")
    @Operation(summary = "Adicionar jogo ao backlog", description = "Valida o jogo no catalogo-service e registra snapshot no backlog do jogador")
    @APIResponse(
            responseCode = "201",
            description = "Item adicionado com sucesso",
            headers = @Header(name = "Location", description = "URI do item criado", schema = @Schema(type = SchemaType.STRING)),
            content = @Content(schema = @Schema(implementation = ItemBacklogResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Erro de validação ou JSON inválido", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Sem permissão para alterar o backlog deste usuário", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "409", description = "Jogo já presente no backlog do usuário", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "422", description = "Jogo inexistente no catálogo", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "502", description = "Erro de integração na chamada ao catálogo", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "503", description = "Catálogo temporariamente indisponível", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public Response adicionar(
            @Parameter(description = "Identificador do jogador", example = "joao")
            @PathParam("usuarioId")
            @Pattern(regexp = PADRAO_USUARIO, message = "usuarioId deve seguir o padrão [A-Za-z0-9._-]{1,64}")
            String usuarioId,
            @Valid @NotNull(message = "corpo da requisição é obrigatório") AdicionarItemRequest requisicao,
            @Context UriInfo uriInfo) {
        autorizador.exigirProprioJogador(usuarioId);
        ItemBacklogResponse criado = backlogService.adicionar(usuarioId, requisicao.jogoId());
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.id())).build();
        return Response.created(location).entity(criado).build();
    }

    @GET
    @RolesAllowed({"JOGADOR", "ADMIN"})
    @Operation(summary = "Listar itens do backlog", description = "Lista os jogos do backlog do jogador, com filtro opcional por status")
    @APIResponse(
            responseCode = "200",
            description = "Lista de itens do backlog",
            content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = ItemBacklogResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Sem permissão para visualizar o backlog de outro jogador", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public List<ItemBacklogResponse> listar(
            @PathParam("usuarioId")
            @Pattern(regexp = PADRAO_USUARIO, message = "usuarioId deve seguir o padrão [A-Za-z0-9._-]{1,64}")
            String usuarioId,
            @QueryParam("status") StatusBacklog status) {
        autorizador.exigirProprioJogadorOuAdmin(usuarioId);
        return backlogService.listar(usuarioId, status);
    }

    @GET
    @Path("/resumo")
    @RolesAllowed({"JOGADOR", "ADMIN"})
    @Operation(summary = "Resumo do backlog", description = "Retorna contagens por status, horas pendentes, horas zeradas e percentual concluído")
    @APIResponse(responseCode = "200", description = "Resumo calculado", content = @Content(schema = @Schema(implementation = ResumoBacklogResponse.class)))
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Sem permissão para visualizar o resumo de outro jogador", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public ResumoBacklogResponse resumo(
            @PathParam("usuarioId")
            @Pattern(regexp = PADRAO_USUARIO, message = "usuarioId deve seguir o padrão [A-Za-z0-9._-]{1,64}")
            String usuarioId) {
        autorizador.exigirProprioJogadorOuAdmin(usuarioId);
        return backlogService.resumoUsuario(usuarioId);
    }

    @GET
    @Path("/{itemId}")
    @RolesAllowed({"JOGADOR", "ADMIN"})
    @Operation(summary = "Buscar item por ID", description = "Retorna os dados locais de um item do backlog")
    @APIResponse(responseCode = "200", description = "Item encontrado", content = @Content(schema = @Schema(implementation = ItemBacklogResponse.class)))
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Sem permissão para acessar item de outro jogador", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "404", description = "Item não encontrado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public ItemBacklogResponse buscarPorId(
            @PathParam("usuarioId")
            @Pattern(regexp = PADRAO_USUARIO, message = "usuarioId deve seguir o padrão [A-Za-z0-9._-]{1,64}")
            String usuarioId,
            @PathParam("itemId") Long itemId) {
        autorizador.exigirProprioJogadorOuAdmin(usuarioId);
        return backlogService.buscarPorId(usuarioId, itemId);
    }

    @GET
    @Path("/{itemId}/detalhes")
    @RolesAllowed({"JOGADOR", "ADMIN"})
    @Operation(
            summary = "Buscar detalhes enriquecidos do item",
            description = "Consulta o catalogo-service para enriquecer plataforma e gênero; em falha do catálogo, devolve fallback degradado com snapshot local"
    )
    @APIResponse(responseCode = "200", description = "Detalhes do item (ao vivo ou degradado)", content = @Content(schema = @Schema(implementation = DetalhesItemBacklogResponse.class)))
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Sem permissão para acessar item de outro jogador", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "404", description = "Item não encontrado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public DetalhesItemBacklogResponse buscarDetalhes(
            @PathParam("usuarioId")
            @Pattern(regexp = PADRAO_USUARIO, message = "usuarioId deve seguir o padrão [A-Za-z0-9._-]{1,64}")
            String usuarioId,
            @PathParam("itemId") Long itemId) {
        autorizador.exigirProprioJogadorOuAdmin(usuarioId);
        return backlogService.buscarDetalhes(usuarioId, itemId);
    }

    @PATCH
    @Path("/{itemId}")
    @RolesAllowed("JOGADOR")
    @Operation(summary = "Alterar status do item", description = "Aplica transição de status validando a máquina de estados e o limite de jogos JOGANDO")
    @APIResponse(responseCode = "200", description = "Status atualizado", content = @Content(schema = @Schema(implementation = ItemBacklogResponse.class)))
    @APIResponse(responseCode = "400", description = "Erro de validação ou JSON inválido", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Sem permissão para alterar backlog de outro jogador", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "404", description = "Item não encontrado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "422", description = "Transição inválida ou limite JOGANDO atingido", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public ItemBacklogResponse alterarStatus(
            @PathParam("usuarioId")
            @Pattern(regexp = PADRAO_USUARIO, message = "usuarioId deve seguir o padrão [A-Za-z0-9._-]{1,64}")
            String usuarioId,
            @PathParam("itemId") Long itemId,
            @Valid @NotNull(message = "corpo da requisição é obrigatório") AlterarStatusRequest requisicao) {
        autorizador.exigirProprioJogador(usuarioId);
        return backlogService.alterarStatus(usuarioId, itemId, requisicao.status());
    }

    @DELETE
    @Path("/{itemId}")
    @RolesAllowed("JOGADOR")
    @Operation(summary = "Remover item do backlog", description = "Remove um jogo do backlog do próprio jogador")
    @APIResponse(responseCode = "204", description = "Item removido com sucesso")
    @APIResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "403", description = "Sem permissão para remover item de outro jogador", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    @APIResponse(responseCode = "404", description = "Item não encontrado", content = @Content(schema = @Schema(implementation = ErroPadrao.class)))
    public Response remover(
            @PathParam("usuarioId")
            @Pattern(regexp = PADRAO_USUARIO, message = "usuarioId deve seguir o padrão [A-Za-z0-9._-]{1,64}")
            String usuarioId,
            @PathParam("itemId") Long itemId) {
        autorizador.exigirProprioJogador(usuarioId);
        backlogService.remover(usuarioId, itemId);
        return Response.noContent().build();
    }
}

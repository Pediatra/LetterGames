package br.com.lettergames.backlog.api.erros;

import br.com.lettergames.backlog.api.ErroPadrao;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Utilitário compartilhado para construção de respostas {@link ErroPadrao} em JSON.
 */
final class ErroPadraoResponseBuilder {

    private ErroPadraoResponseBuilder() {
    }

    static Response construir(int status, String codigo, String mensagem, UriInfo uriInfo) {
        return construirComDetalhes(status, codigo, mensagem, uriInfo, List.of());
    }

    static Response construirComDetalhes(
            int status, String codigo, String mensagem, UriInfo uriInfo, List<ErroPadrao.CampoErro> detalhes) {
        ErroPadrao body = ErroPadrao.comDetalhes(status, codigo, mensagem, caminho(uriInfo), detalhes);
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(body)
                .build();
    }

    static String caminho(UriInfo uriInfo) {
        if (uriInfo == null || uriInfo.getPath() == null || uriInfo.getPath().isBlank()) {
            return "/";
        }
        String path = uriInfo.getPath();
        return path.startsWith("/") ? path : "/" + path;
    }
}

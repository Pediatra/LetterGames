package br.com.lettergames.backlog.api;

import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Corpo único padronizado para todas as respostas de erro da API REST.
 */
@Schema(name = "ErroPadrao", description = "Formato único de erro retornado pelos endpoints da API")
public record ErroPadrao(
        @Schema(example = "400") int status,
        @Schema(example = "ERRO_VALIDACAO") String codigo,
        @Schema(example = "Dados da requisição inválidos") String mensagem,
        @Schema(example = "/usuarios/joao/backlog") String caminho,
        Instant timestamp,
        List<CampoErro> detalhes) {

    public ErroPadrao {
        detalhes = detalhes == null ? List.of() : List.copyOf(detalhes);
    }

    public static ErroPadrao de(int status, String codigo, String mensagem, String caminho) {
        return new ErroPadrao(status, codigo, mensagem, caminho, Instant.now(), List.of());
    }

    public static ErroPadrao comDetalhes(
            int status, String codigo, String mensagem, String caminho, List<CampoErro> detalhes) {
        return new ErroPadrao(status, codigo, mensagem, caminho, Instant.now(), detalhes);
    }

    @Schema(name = "CampoErro", description = "Detalhe de erro associado a um campo da requisição")
    public record CampoErro(String campo, String mensagem) {
    }
}

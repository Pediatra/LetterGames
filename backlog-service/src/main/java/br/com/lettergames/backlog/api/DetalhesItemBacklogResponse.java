package br.com.lettergames.backlog.api;

import br.com.lettergames.backlog.dominio.ItemBacklog;
import br.com.lettergames.backlog.dominio.StatusBacklog;
import br.com.lettergames.backlog.integracao.JogoCatalogoDTO;
import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Visão enriquecida de um item do backlog com dados atuais do catálogo ou fallback para o snapshot local.
 */
@Schema(name = "DetalhesItemBacklogResponse", description = "Item enriquecido com detalhes do catálogo ou snapshot degradado")
public record DetalhesItemBacklogResponse(
        Long id,
        String usuarioId,
        Long jogoId,
        String titulo,
        String plataforma,
        String genero,
        Integer duracaoMediaHoras,
        StatusBacklog status,
        boolean catalogoDisponivel,
        String aviso,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static DetalhesItemBacklogResponse comCatalogo(ItemBacklog item, JogoCatalogoDTO jogo) {
        return new DetalhesItemBacklogResponse(
                item.getId(),
                item.getUsuarioId(),
                item.getJogoId(),
                jogo.titulo(),
                jogo.plataforma(),
                jogo.genero(),
                jogo.duracaoMediaHoras(),
                item.getStatus(),
                true,
                null,
                item.getCriadoEm(),
                item.getAtualizadoEm()
        );
    }

    public static DetalhesItemBacklogResponse degradado(ItemBacklog item, String aviso) {
        return new DetalhesItemBacklogResponse(
                item.getId(),
                item.getUsuarioId(),
                item.getJogoId(),
                item.getTitulo(),
                null,
                null,
                item.getDuracaoMediaHoras(),
                item.getStatus(),
                false,
                aviso,
                item.getCriadoEm(),
                item.getAtualizadoEm()
        );
    }
}

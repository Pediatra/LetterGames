package br.com.lettergames.backlog.dominio;

/**
 * Lançada quando o item não existe no backlog do usuário informado (HTTP 404).
 */
public class ItemNaoEncontradoException extends DominioBacklogException {

    public ItemNaoEncontradoException(String usuarioId, Long itemId) {
        super(404, "ITEM_NAO_ENCONTRADO",
                "Item " + itemId + " não encontrado no backlog de '" + usuarioId + "'");
    }
}

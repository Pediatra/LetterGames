package br.com.lettergames.backlog.dominio;

import br.com.lettergames.backlog.api.ItemBacklogResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Operações transacionais isoladas do backlog, separadas das chamadas REST para não reter conexões
 * de banco durante retries ou timeouts de rede.
 */
@ApplicationScoped
public class BacklogTransacionalService {

    private final ItemBacklogRepository repository;
    private final int limiteJogando;

    @Inject
    public BacklogTransacionalService(
            ItemBacklogRepository repository,
            @ConfigProperty(name = "backlog.limite-jogando") int limiteJogando) {
        this.repository = repository;
        this.limiteJogando = limiteJogando;
    }

    @Transactional
    public ItemBacklogResponse salvarNovoItem(
            String usuarioId, Long jogoId, String titulo, Integer duracaoMediaHoras) {
        if (repository.existePorUsuarioEJogo(usuarioId, jogoId)) {
            throw new JogoJaNoBacklogException(usuarioId, jogoId);
        }
        ItemBacklog item = ItemBacklog.novo(usuarioId, jogoId, titulo, duracaoMediaHoras);
        try {
            repository.persistAndFlush(item);
        } catch (PersistenceException e) {
            if (repository.existePorUsuarioEJogo(usuarioId, jogoId)) {
                throw new JogoJaNoBacklogException(usuarioId, jogoId);
            }
            throw e;
        }
        return ItemBacklogResponse.de(item);
    }

    @Transactional
    public ItemBacklogResponse alterarStatus(String usuarioId, Long itemId, StatusBacklog novoStatus) {
        ItemBacklog item = repository.buscarPorIdEUsuario(itemId, usuarioId)
                .orElseThrow(() -> new ItemNaoEncontradoException(usuarioId, itemId));

        TransicoesStatus.validar(item.getStatus(), novoStatus);
        if (novoStatus == StatusBacklog.JOGANDO
                && repository.contarPorUsuarioEStatus(usuarioId, StatusBacklog.JOGANDO) >= limiteJogando) {
            throw new LimiteJogandoAtingidoException(usuarioId, limiteJogando);
        }

        item.alterarStatus(novoStatus);
        return ItemBacklogResponse.de(item);
    }

    @Transactional
    public void remover(String usuarioId, Long itemId) {
        ItemBacklog item = repository.buscarPorIdEUsuario(itemId, usuarioId)
                .orElseThrow(() -> new ItemNaoEncontradoException(usuarioId, itemId));
        repository.delete(item);
    }
}

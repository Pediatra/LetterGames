package br.com.lettergames.backlog.dominio;

import br.com.lettergames.backlog.api.DetalhesItemBacklogResponse;
import br.com.lettergames.backlog.api.EstatisticasGlobaisResponse;
import br.com.lettergames.backlog.api.ItemBacklogResponse;
import br.com.lettergames.backlog.api.ResumoBacklogResponse;
import br.com.lettergames.backlog.integracao.CatalogoIntegracaoService;
import br.com.lettergames.backlog.integracao.JogoCatalogoDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Casos de uso do backlog de jogos, coordenando integração com o catálogo, persistência e métricas.
 */
@Startup
@ApplicationScoped
public class BacklogService {

    private static final Logger LOG = Logger.getLogger(BacklogService.class);

    private final ItemBacklogRepository repository;
    private final BacklogTransacionalService transacionalService;
    private final CatalogoIntegracaoService catalogoIntegracao;
    private final Counter contadorItensAdicionados;
    private final Counter contadorJogosZerados;

    @Inject
    public BacklogService(
            ItemBacklogRepository repository,
            BacklogTransacionalService transacionalService,
            CatalogoIntegracaoService catalogoIntegracao,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.transacionalService = transacionalService;
        this.catalogoIntegracao = catalogoIntegracao;
        this.contadorItensAdicionados = Counter.builder("lettergames_backlog_itens_adicionados_total")
                .description("Total de jogos adicionados aos backlogs")
                .register(meterRegistry);
        this.contadorJogosZerados = Counter.builder("lettergames_backlog_jogos_zerados_total")
                .description("Total de jogos marcados como ZERADO")
                .register(meterRegistry);
    }

    public ItemBacklogResponse adicionar(String usuarioId, Long jogoId) {
        if (repository.existePorUsuarioEJogo(usuarioId, jogoId)) {
            throw new JogoJaNoBacklogException(usuarioId, jogoId);
        }

        JogoCatalogoDTO jogo = catalogoIntegracao.consultarJogoObrigatorio(jogoId);
        ItemBacklogResponse salvo = transacionalService.salvarNovoItem(
                usuarioId, jogoId, jogo.titulo(), jogo.duracaoMediaHoras());
        contadorItensAdicionados.increment();
        LOG.infof("Item adicionado ao backlog: usuario=%s jogoId=%d itemId=%d",
                usuarioId, jogoId, salvo.id());
        return salvo;
    }

    public ItemBacklogResponse alterarStatus(String usuarioId, Long itemId, StatusBacklog novoStatus) {
        ItemBacklogResponse atualizado = transacionalService.alterarStatus(usuarioId, itemId, novoStatus);
        if (novoStatus == StatusBacklog.ZERADO) {
            contadorJogosZerados.increment();
        }
        LOG.infof("Status alterado no backlog: usuario=%s itemId=%d novoStatus=%s",
                usuarioId, itemId, novoStatus);
        return atualizado;
    }

    public void remover(String usuarioId, Long itemId) {
        transacionalService.remover(usuarioId, itemId);
        LOG.infof("Item removido do backlog: usuario=%s itemId=%d", usuarioId, itemId);
    }

    @Transactional
    public List<ItemBacklogResponse> listar(String usuarioId, StatusBacklog status) {
        return repository.listarPorUsuario(usuarioId, status).stream()
                .map(ItemBacklogResponse::de)
                .toList();
    }

    @Transactional
    public ItemBacklogResponse buscarPorId(String usuarioId, Long itemId) {
        ItemBacklog item = buscarItemOuFalhar(usuarioId, itemId);
        return ItemBacklogResponse.de(item);
    }

    public DetalhesItemBacklogResponse buscarDetalhes(String usuarioId, Long itemId) {
        ItemBacklog item = buscarItemOuFalhar(usuarioId, itemId);
        return catalogoIntegracao.consultarDetalhesComSnapshot(item);
    }

    @Transactional
    public ResumoBacklogResponse resumoUsuario(String usuarioId) {
        return repository.calcularResumoUsuario(usuarioId);
    }

    @Transactional
    public EstatisticasGlobaisResponse estatisticasGlobais() {
        return repository.calcularEstatisticasGerais();
    }

    @Transactional
    ItemBacklog buscarItemOuFalhar(String usuarioId, Long itemId) {
        return repository.buscarPorIdEUsuario(itemId, usuarioId)
                .orElseThrow(() -> new ItemNaoEncontradoException(usuarioId, itemId));
    }
}

package br.com.lettergames.backlog.integracao;

import br.com.lettergames.backlog.api.DetalhesItemBacklogResponse;
import br.com.lettergames.backlog.dominio.CatalogoIndisponivelException;
import br.com.lettergames.backlog.dominio.CatalogoRequisicaoInvalidaException;
import br.com.lettergames.backlog.dominio.ItemBacklog;
import br.com.lettergames.backlog.dominio.JogoInexistenteNoCatalogoException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.temporal.ChronoUnit;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Fachada CDI que aplica Timeout, Retry, CircuitBreaker e Fallback sobre {@link CatalogoRestClient}.
 *
 * <p>As anotações residem em um bean CDI separado da interface {@code @RegisterRestClient} para que os
 * interceptadores SmallRye Fault Tolerance continuem ativos mesmo quando o cliente for mockado em testes.
 */
@Startup
@ApplicationScoped
public class CatalogoIntegracaoService {

    private static final Logger LOG = Logger.getLogger(CatalogoIntegracaoService.class);
    static final String AVISO_DEGRADACAO =
            "Catálogo temporariamente indisponível; exibindo dados do snapshot local.";

    private final CatalogoRestClient catalogoRestClient;
    private final Counter contadorFalhasCatalogo;

    @Inject
    public CatalogoIntegracaoService(
            @RestClient CatalogoRestClient catalogoRestClient,
            MeterRegistry meterRegistry) {
        this.catalogoRestClient = catalogoRestClient;
        this.contadorFalhasCatalogo = Counter.builder("lettergames_backlog_catalogo_falhas_total")
                .description("Total de falhas ou aberturas de fallback ao consultar o catalogo-service")
                .register(meterRegistry);
    }

    @Timeout(value = 2000, unit = ChronoUnit.MILLIS)
    @Retry(
            maxRetries = 2,
            delay = 200,
            jitter = 50,
            abortOn = {
                    JogoInexistenteNoCatalogoException.class,
                    CatalogoRequisicaoInvalidaException.class,
                    CircuitBreakerOpenException.class
            }
    )
    @CircuitBreaker(
            requestVolumeThreshold = 6,
            failureRatio = 0.5,
            delay = 10000,
            successThreshold = 2,
            skipOn = {
                    JogoInexistenteNoCatalogoException.class,
                    CatalogoRequisicaoInvalidaException.class
            }
    )
    @Fallback(
            fallbackMethod = "fallbackJogoObrigatorio",
            skipOn = {
                    JogoInexistenteNoCatalogoException.class,
                    CatalogoRequisicaoInvalidaException.class
            }
    )
    public JogoCatalogoDTO consultarJogoObrigatorio(Long jogoId) {
        JogoCatalogoDTO jogo = catalogoRestClient.buscarPorId(jogoId);
        if (jogo == null) {
            throw new CatalogoRemotoException("Catálogo retornou corpo vazio para o jogo " + jogoId);
        }
        return jogo;
    }

    @SuppressWarnings("unused")
    JogoCatalogoDTO fallbackJogoObrigatorio(Long jogoId) {
        contadorFalhasCatalogo.increment();
        LOG.warnf("Fallback acionado na criação do backlog para jogoId=%d; recusando escrita com 503", jogoId);
        throw new CatalogoIndisponivelException(null);
    }

    @Timeout(value = 2000, unit = ChronoUnit.MILLIS)
    @Retry(
            maxRetries = 2,
            delay = 200,
            jitter = 50,
            abortOn = {
                    JogoInexistenteNoCatalogoException.class,
                    CatalogoRequisicaoInvalidaException.class,
                    CircuitBreakerOpenException.class
            }
    )
    @CircuitBreaker(
            requestVolumeThreshold = 6,
            failureRatio = 0.5,
            delay = 10000,
            successThreshold = 2,
            skipOn = {
                    JogoInexistenteNoCatalogoException.class,
                    CatalogoRequisicaoInvalidaException.class
            }
    )
    @Fallback(
            fallbackMethod = "fallbackDetalhesComSnapshot",
            skipOn = {
                    JogoInexistenteNoCatalogoException.class,
                    CatalogoRequisicaoInvalidaException.class
            }
    )
    public DetalhesItemBacklogResponse consultarDetalhesComSnapshot(ItemBacklog item) {
        JogoCatalogoDTO jogo = catalogoRestClient.buscarPorId(item.getJogoId());
        if (jogo == null) {
            throw new CatalogoRemotoException("Catálogo retornou corpo vazio para o jogo " + item.getJogoId());
        }
        return DetalhesItemBacklogResponse.comCatalogo(item, jogo);
    }

    @SuppressWarnings("unused")
    DetalhesItemBacklogResponse fallbackDetalhesComSnapshot(ItemBacklog item) {
        contadorFalhasCatalogo.increment();
        LOG.warnf("Fallback degradado acionado para itemId=%d (jogoId=%d); devolvendo snapshot local",
                item.getId(), item.getJogoId());
        return DetalhesItemBacklogResponse.degradado(item, AVISO_DEGRADACAO);
    }
}

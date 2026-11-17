package br.com.lettergames.backlog.infra;

import br.com.lettergames.backlog.dominio.ItemBacklogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

/**
 * Sonda de readiness que verifica a conectividade com o banco H2 do {@code backlog-service}.
 */
@Readiness
@ApplicationScoped
public class BacklogReadinessCheck implements HealthCheck {

    private final ItemBacklogRepository itemRepository;
    private final int limiteJogando;

    @Inject
    public BacklogReadinessCheck(
            ItemBacklogRepository itemRepository,
            @ConfigProperty(name = "backlog.limite-jogando") int limiteJogando) {
        this.itemRepository = itemRepository;
        this.limiteJogando = limiteJogando;
    }

    @Override
    @Transactional
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("backlog-banco-h2");
        try {
            long totalItens = itemRepository.count();
            return builder.up()
                    .withData("itensBacklog", totalItens)
                    .withData("limiteJogando", limiteJogando)
                    .build();
        } catch (Exception e) {
            return builder.down()
                    .withData("motivo", "Falha ao consultar o repositório do backlog")
                    .build();
        }
    }
}

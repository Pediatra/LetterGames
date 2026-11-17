package br.com.lettergames.catalogo.infra;

import br.com.lettergames.catalogo.dominio.JogoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

/**
 * Sonda de readiness que verifica a conectividade com o banco H2 do {@code catalogo-service}.
 */
@Readiness
@ApplicationScoped
public class CatalogoReadinessCheck implements HealthCheck {

    private final JogoRepository jogoRepository;
    private final int duracaoMaximaHoras;

    @Inject
    public CatalogoReadinessCheck(
            JogoRepository jogoRepository,
            @ConfigProperty(name = "catalogo.duracao-maxima-horas") int duracaoMaximaHoras) {
        this.jogoRepository = jogoRepository;
        this.duracaoMaximaHoras = duracaoMaximaHoras;
    }

    @Override
    @Transactional
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("catalogo-banco-h2");
        try {
            long totalJogos = jogoRepository.count();
            return builder.up()
                    .withData("jogosCadastrados", totalJogos)
                    .withData("duracaoMaximaHoras", duracaoMaximaHoras)
                    .build();
        } catch (Exception e) {
            return builder.down()
                    .withData("motivo", "Falha ao consultar o repositório do catálogo")
                    .build();
        }
    }
}

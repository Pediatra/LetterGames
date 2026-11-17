package br.com.lettergames.catalogo.infra;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Sonda de liveness do {@code catalogo-service}.
 */
@Liveness
@ApplicationScoped
public class CatalogoLivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("catalogo-service-vivo");
    }
}

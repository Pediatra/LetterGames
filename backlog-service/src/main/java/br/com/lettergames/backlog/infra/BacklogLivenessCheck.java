package br.com.lettergames.backlog.infra;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Sonda de liveness do {@code backlog-service}.
 */
@Liveness
@ApplicationScoped
public class BacklogLivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("backlog-service-vivo");
    }
}

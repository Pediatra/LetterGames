package br.com.lettergames.backlog.seguranca;

import br.com.lettergames.backlog.dominio.AcessoNegadoBacklogException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Aplica regra de posse de dados sobre o usuário autenticado além dos papéis RBAC.
 */
@ApplicationScoped
public class AutorizadorBacklog {

    private final SecurityIdentity securityIdentity;

    @Inject
    public AutorizadorBacklog(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    public void exigirProprioJogador(String usuarioId) {
        if (!ehProprioUsuario(usuarioId)) {
            throw new AcessoNegadoBacklogException(usuarioId);
        }
    }

    public void exigirProprioJogadorOuAdmin(String usuarioId) {
        if (!securityIdentity.hasRole("ADMIN") && !ehProprioUsuario(usuarioId)) {
            throw new AcessoNegadoBacklogException(usuarioId);
        }
    }

    private boolean ehProprioUsuario(String usuarioId) {
        return !securityIdentity.isAnonymous()
                && securityIdentity.getPrincipal() != null
                && usuarioId.equals(securityIdentity.getPrincipal().getName());
    }
}

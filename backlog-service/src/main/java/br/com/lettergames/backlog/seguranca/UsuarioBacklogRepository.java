package br.com.lettergames.backlog.seguranca;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Repositório Panache para consultas de usuários de autenticação do backlog.
 */
@ApplicationScoped
public class UsuarioBacklogRepository implements PanacheRepository<UsuarioBacklog> {

    public Optional<UsuarioBacklog> buscarPorUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}

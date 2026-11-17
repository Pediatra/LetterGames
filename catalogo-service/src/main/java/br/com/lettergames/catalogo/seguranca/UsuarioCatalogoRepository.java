package br.com.lettergames.catalogo.seguranca;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Repositório Panache para consultas de usuários de autenticação do catálogo.
 */
@ApplicationScoped
public class UsuarioCatalogoRepository implements PanacheRepository<UsuarioCatalogo> {

    public Optional<UsuarioCatalogo> buscarPorUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}

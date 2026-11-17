package br.com.lettergames.backlog.seguranca;

import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuário persistido para autenticação JPA com hash BCrypt no {@code backlog-service}.
 */
@Entity
@Table(name = "usuarios_backlog")
@UserDefinition
public class UsuarioBacklog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Username
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Password
    @Column(name = "senha_hash", nullable = false, length = 120)
    private String senhaHash;

    @Roles
    @Column(nullable = false, length = 64)
    private String papel;

    protected UsuarioBacklog() {
    }

    public UsuarioBacklog(String username, String senhaHash, String papel) {
        this.username = username;
        this.senhaHash = senhaHash;
        this.papel = papel;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getPapel() {
        return papel;
    }
}

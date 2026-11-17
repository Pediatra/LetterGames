package br.com.lettergames.catalogo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidade JPA de jogo cadastrado no catálogo.
 */
@Entity
@Table(
        name = "jogos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jogos_titulo_plataforma",
                columnNames = {"titulo", "plataforma"}
        )
)
public class Jogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 50)
    private String plataforma;

    @Column(nullable = false, length = 50)
    private String genero;

    @Column(name = "duracao_media_horas", nullable = false)
    private Integer duracaoMediaHoras;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Jogo() {
    }

    public Jogo(String titulo, String plataforma, String genero, Integer duracaoMediaHoras) {
        atualizar(titulo, plataforma, genero, duracaoMediaHoras);
    }

    public void atualizar(String titulo, String plataforma, String genero, Integer duracaoMediaHoras) {
        this.titulo = Objects.requireNonNull(titulo).trim();
        this.plataforma = Objects.requireNonNull(plataforma).trim();
        this.genero = Objects.requireNonNull(genero).trim();
        this.duracaoMediaHoras = Objects.requireNonNull(duracaoMediaHoras);
        this.atualizadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public String getGenero() {
        return genero;
    }

    public Integer getDuracaoMediaHoras() {
        return duracaoMediaHoras;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}

package br.com.lettergames.backlog.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

/**
 * Jogo associado ao backlog de um usuário, mantendo snapshot local de título e duração média.
 */
@Entity
@Table(
        name = "itens_backlog",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_itens_backlog_usuario_jogo",
                columnNames = {"usuario_id", "jogo_id"}
        )
)
public class ItemBacklog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, length = 64)
    private String usuarioId;

    @Column(name = "jogo_id", nullable = false)
    private Long jogoId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "duracao_media_horas")
    private Integer duracaoMediaHoras;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusBacklog status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(nullable = false)
    private long versao;

    protected ItemBacklog() {
    }

    private ItemBacklog(String usuarioId, Long jogoId, String titulo, Integer duracaoMediaHoras, Instant agora) {
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.jogoId = Objects.requireNonNull(jogoId);
        this.titulo = Objects.requireNonNull(titulo);
        this.duracaoMediaHoras = duracaoMediaHoras;
        this.status = StatusBacklog.QUERO_JOGAR;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public static ItemBacklog novo(String usuarioId, Long jogoId, String titulo, Integer duracaoMediaHoras) {
        return new ItemBacklog(usuarioId, jogoId, titulo, duracaoMediaHoras, Instant.now());
    }

    public StatusBacklog alterarStatus(StatusBacklog novo) {
        Objects.requireNonNull(novo);
        TransicoesStatus.validar(this.status, novo);
        StatusBacklog anterior = this.status;
        this.status = novo;
        this.atualizadoEm = Instant.now();
        return anterior;
    }

    public Long getId() {
        return id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public Long getJogoId() {
        return jogoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public Integer getDuracaoMediaHoras() {
        return duracaoMediaHoras;
    }

    public StatusBacklog getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public long getVersao() {
        return versao;
    }
}

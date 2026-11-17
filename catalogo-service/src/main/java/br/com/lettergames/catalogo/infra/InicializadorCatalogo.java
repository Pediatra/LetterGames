package br.com.lettergames.catalogo.infra;

import br.com.lettergames.catalogo.dominio.Jogo;
import br.com.lettergames.catalogo.dominio.JogoRepository;
import br.com.lettergames.catalogo.seguranca.UsuarioCatalogo;
import br.com.lettergames.catalogo.seguranca.UsuarioCatalogoRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Carrega usuários de teste (BCrypt) e catálogo inicial apenas quando {@code lettergames.seed.habilitado=true}.
 */
@ApplicationScoped
public class InicializadorCatalogo {

    private static final Logger LOG = Logger.getLogger(InicializadorCatalogo.class);

    private final UsuarioCatalogoRepository usuarioRepository;
    private final JogoRepository jogoRepository;
    private final boolean seedHabilitado;

    @Inject
    public InicializadorCatalogo(
            UsuarioCatalogoRepository usuarioRepository,
            JogoRepository jogoRepository,
            @ConfigProperty(name = "lettergames.seed.habilitado", defaultValue = "false") boolean seedHabilitado) {
        this.usuarioRepository = usuarioRepository;
        this.jogoRepository = jogoRepository;
        this.seedHabilitado = seedHabilitado;
    }

    @Transactional
    void aoIniciar(@Observes StartupEvent evento) {
        if (!seedHabilitado) {
            LOG.info("Seed do catalogo-service desabilitado neste perfil.");
            return;
        }

        if (usuarioRepository.count() == 0) {
            List<UsuarioCatalogo> usuarios = List.of(
                    new UsuarioCatalogo("admin", BcryptUtil.bcryptHash("admin123"), "ADMIN"),
                    new UsuarioCatalogo("joao", BcryptUtil.bcryptHash("joao123"), "JOGADOR"),
                    new UsuarioCatalogo("maria", BcryptUtil.bcryptHash("maria123"), "JOGADOR"),
                    new UsuarioCatalogo("sistema-backlog", BcryptUtil.bcryptHash("sistema123"), "JOGADOR")
            );
            usuarioRepository.persist(usuarios);
            LOG.infof("Seed de usuários do catálogo carregado: %d registros", usuarios.size());
        }

        if (jogoRepository.count() == 0) {
            List<Jogo> jogos = List.of(
                    new Jogo("Hollow Knight", "PC", "Metroidvania", 27),
                    new Jogo("The Witcher 3", "PC", "RPG", 103),
                    new Jogo("Celeste", "Switch", "Plataforma", 12),
                    new Jogo("Elden Ring", "PS5", "Soulslike", 98),
                    new Jogo("Hades", "PC", "Roguelike", 43),
                    new Jogo("Stardew Valley", "PC", "Simulação", 95),
                    new Jogo("God of War Ragnarök", "PS5", "Ação", 51),
                    new Jogo("Zelda: Breath of the Wild", "Switch", "Aventura", 98),
                    new Jogo("Baldur's Gate 3", "PC", "RPG", 117),
                    new Jogo("Portal 2", "PC", "Puzzle", 14)
            );
            jogoRepository.persist(jogos);
            LOG.infof("Seed de jogos do catálogo carregado: %d registros", jogos.size());
        }
    }
}

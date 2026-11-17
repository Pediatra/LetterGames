package br.com.lettergames.backlog.infra;

import br.com.lettergames.backlog.seguranca.UsuarioBacklog;
import br.com.lettergames.backlog.seguranca.UsuarioBacklogRepository;
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
 * Carrega usuários fictícios de teste (BCrypt) apenas quando {@code lettergames.seed.habilitado=true}.
 */
@ApplicationScoped
public class InicializadorBacklog {

    private static final Logger LOG = Logger.getLogger(InicializadorBacklog.class);

    private final UsuarioBacklogRepository usuarioRepository;
    private final boolean seedHabilitado;

    @Inject
    public InicializadorBacklog(
            UsuarioBacklogRepository usuarioRepository,
            @ConfigProperty(name = "lettergames.seed.habilitado", defaultValue = "false") boolean seedHabilitado) {
        this.usuarioRepository = usuarioRepository;
        this.seedHabilitado = seedHabilitado;
    }

    @Transactional
    void aoIniciar(@Observes StartupEvent evento) {
        if (!seedHabilitado) {
            LOG.info("Seed do backlog-service desabilitado neste perfil.");
            return;
        }

        if (usuarioRepository.count() == 0) {
            List<UsuarioBacklog> usuarios = List.of(
                    new UsuarioBacklog("admin", BcryptUtil.bcryptHash("admin123"), "ADMIN"),
                    new UsuarioBacklog("joao", BcryptUtil.bcryptHash("joao123"), "JOGADOR"),
                    new UsuarioBacklog("maria", BcryptUtil.bcryptHash("maria123"), "JOGADOR")
            );
            usuarioRepository.persist(usuarios);
            LOG.infof("Seed de usuários do backlog carregado: %d registros", usuarios.size());
        }
    }
}

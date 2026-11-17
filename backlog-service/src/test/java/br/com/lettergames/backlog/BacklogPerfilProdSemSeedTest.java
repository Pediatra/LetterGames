package br.com.lettergames.backlog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.lettergames.backlog.dominio.ItemBacklogRepository;
import br.com.lettergames.backlog.seguranca.UsuarioBacklogRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BacklogPerfilProdSemSeedTest.SemSeedProfile.class)
class BacklogPerfilProdSemSeedTest {

    @Inject
    ItemBacklogRepository itemBacklogRepository;

    @Inject
    UsuarioBacklogRepository usuarioRepository;

    @Test
    void naoDeveCarregarDadosDeExemploQuandoSeedDesabilitado() {
        assertEquals(0, usuarioRepository.count());
        assertEquals(0, itemBacklogRepository.count());
    }

    public static class SemSeedProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "lettergames.seed.habilitado", "false",
                    "quarkus.datasource.jdbc.url", "jdbc:h2:mem:backlog_sem_seed;DB_CLOSE_DELAY=-1"
            );
        }
    }
}

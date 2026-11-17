package br.com.lettergames.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.lettergames.catalogo.dominio.JogoRepository;
import br.com.lettergames.catalogo.seguranca.UsuarioCatalogoRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(CatalogoPerfilProdSemSeedTest.SemSeedProfile.class)
class CatalogoPerfilProdSemSeedTest {

    @Inject
    JogoRepository jogoRepository;

    @Inject
    UsuarioCatalogoRepository usuarioRepository;

    @Test
    void naoDeveCarregarSeedQuandoPerfilDesabilitaDadosDeExemplo() {
        assertEquals(0, jogoRepository.count());
        assertEquals(0, usuarioRepository.count());
    }

    public static class SemSeedProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "lettergames.seed.habilitado", "false",
                    "quarkus.datasource.jdbc.url", "jdbc:h2:mem:catalogo_sem_seed;DB_CLOSE_DELAY=-1"
            );
        }
    }
}

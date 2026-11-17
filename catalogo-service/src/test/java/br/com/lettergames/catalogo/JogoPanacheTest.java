package br.com.lettergames.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.lettergames.catalogo.api.JogoRequest;
import br.com.lettergames.catalogo.api.JogoResponse;
import br.com.lettergames.catalogo.dominio.Jogo;
import br.com.lettergames.catalogo.dominio.JogoRepository;
import br.com.lettergames.catalogo.dominio.JogoService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JogoPanacheTest {

    @Inject
    JogoRepository jogoRepository;

    @Inject
    JogoService jogoService;

    @Test
    void devePersistirConsultarComFiltrosOrdenacaoEContagem() {
        long antesXbox = jogoRepository.contarPorPlataforma("Xbox Series");

        JogoResponse zeldaClone = jogoService.criar(
                new JogoRequest("Zeta Quest Panache", "Xbox Series", "Indie", 11));
        JogoResponse alfa = jogoService.criar(
                new JogoRequest("Alfa Quest Panache", "Xbox Series", "Indie", 9));

        try {
            List<Jogo> filtrados = jogoRepository.listarComFiltros("xbox series", "indie", "quest panache");
            assertEquals(2, filtrados.size());
            assertEquals("Alfa Quest Panache", filtrados.get(0).getTitulo());
            assertEquals("Zeta Quest Panache", filtrados.get(1).getTitulo());

            assertTrue(jogoRepository.existePorTituloEPlataforma("alfa quest panache", "XBOX SERIES", null));
            assertFalse(jogoRepository.existePorTituloEPlataforma("alfa quest panache", "XBOX SERIES", alfa.id()));
            assertEquals(antesXbox + 2, jogoRepository.contarPorPlataforma("Xbox Series"));
        } finally {
            jogoService.remover(alfa.id());
            jogoService.remover(zeldaClone.id());
        }
    }
}

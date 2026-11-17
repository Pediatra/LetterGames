package br.com.lettergames.backlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import br.com.lettergames.backlog.api.EstatisticasGlobaisResponse;
import br.com.lettergames.backlog.api.ItemBacklogResponse;
import br.com.lettergames.backlog.api.ResumoBacklogResponse;
import br.com.lettergames.backlog.dominio.BacklogService;
import br.com.lettergames.backlog.dominio.ItemBacklogRepository;
import br.com.lettergames.backlog.dominio.StatusBacklog;
import br.com.lettergames.backlog.integracao.CatalogoRestClient;
import br.com.lettergames.backlog.integracao.JogoCatalogoDTO;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BacklogPanacheTest {

    @InjectMock
    @RestClient
    CatalogoRestClient catalogoRestClient;

    @Inject
    BacklogService backlogService;

    @Inject
    ItemBacklogRepository itemBacklogRepository;

    @BeforeEach
    void limpar() {
        QuarkusTransaction.requiringNew().run(() -> itemBacklogRepository.deleteAll());
    }

    @Test
    void devePersistirFiltrarECalcularResumoEEstatisticasViaAgregacaoPanache() {
        when(catalogoRestClient.buscarPorId(1L))
                .thenReturn(new JogoCatalogoDTO(1L, "Hollow Knight", "PC", "Metroidvania", 27));
        when(catalogoRestClient.buscarPorId(3L))
                .thenReturn(new JogoCatalogoDTO(3L, "Celeste", "Switch", "Plataforma", 12));
        when(catalogoRestClient.buscarPorId(5L))
                .thenReturn(new JogoCatalogoDTO(5L, "Hades", "PC", "Roguelike", 43));
        when(catalogoRestClient.buscarPorId(6L))
                .thenReturn(new JogoCatalogoDTO(6L, "Stardew Valley", "PC", "Simulação", 95));
        when(catalogoRestClient.buscarPorId(10L))
                .thenReturn(new JogoCatalogoDTO(10L, "Portal 2", "PC", "Puzzle", 14));

        // João: 1 QUERO_JOGAR (27h), 1 JOGANDO (43h), 1 ZERADO (12h), 1 ABANDONADO (95h)
        backlogService.adicionar("joao", 1L);
        ItemBacklogResponse celeste = backlogService.adicionar("joao", 3L);
        backlogService.alterarStatus("joao", celeste.id(), StatusBacklog.JOGANDO);
        backlogService.alterarStatus("joao", celeste.id(), StatusBacklog.ZERADO);

        ItemBacklogResponse hades = backlogService.adicionar("joao", 5L);
        backlogService.alterarStatus("joao", hades.id(), StatusBacklog.JOGANDO);

        ItemBacklogResponse stardew = backlogService.adicionar("joao", 6L);
        backlogService.alterarStatus("joao", stardew.id(), StatusBacklog.ABANDONADO);

        // Maria: 1 ZERADO (14h)
        ItemBacklogResponse portal = backlogService.adicionar("maria", 10L);
        backlogService.alterarStatus("maria", portal.id(), StatusBacklog.JOGANDO);
        backlogService.alterarStatus("maria", portal.id(), StatusBacklog.ZERADO);

        List<ItemBacklogResponse> todosJoao = backlogService.listar("joao", null);
        assertEquals(4, todosJoao.size());

        List<ItemBacklogResponse> jogandoJoao = backlogService.listar("joao", StatusBacklog.JOGANDO);
        assertEquals(1, jogandoJoao.size());
        assertEquals("Hades", jogandoJoao.get(0).titulo());

        ResumoBacklogResponse resumoJoao = backlogService.resumoUsuario("joao");
        assertEquals(4, resumoJoao.total());
        assertEquals(1, resumoJoao.porStatus().get(StatusBacklog.QUERO_JOGAR));
        assertEquals(1, resumoJoao.porStatus().get(StatusBacklog.JOGANDO));
        assertEquals(1, resumoJoao.porStatus().get(StatusBacklog.ZERADO));
        assertEquals(1, resumoJoao.porStatus().get(StatusBacklog.ABANDONADO));
        assertEquals(70, resumoJoao.horasPendentes());
        assertEquals(12, resumoJoao.horasZeradas());
        assertEquals(25.0, resumoJoao.percentualZerado());

        EstatisticasGlobaisResponse globais = backlogService.estatisticasGlobais();
        assertEquals(2, globais.totalJogadoresComBacklog());
        assertEquals(5, globais.totalItens());
        assertEquals(70, globais.horasPendentes());
        assertEquals(26, globais.horasZeradas());
        assertEquals(40.0, globais.percentualZerado());
    }
}

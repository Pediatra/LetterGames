package br.com.lettergames.backlog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.lettergames.backlog.dominio.ItemBacklogRepository;
import br.com.lettergames.backlog.dominio.JogoInexistenteNoCatalogoException;
import br.com.lettergames.backlog.integracao.CatalogoRemotoException;
import br.com.lettergames.backlog.integracao.CatalogoRestClient;
import br.com.lettergames.backlog.integracao.JogoCatalogoDTO;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CatalogoResilienciaTest {

    @InjectMock
    @RestClient
    CatalogoRestClient catalogoRestClient;

    @Inject
    ItemBacklogRepository itemBacklogRepository;

    @Inject
    CircuitBreakerMaintenance cbMaintenance;

    @BeforeEach
    void preparar() {
        QuarkusTransaction.requiringNew().run(() -> itemBacklogRepository.deleteAll());
        cbMaintenance.resetAll();
    }

    @Test
    void deveRecuperarDeFalhaTransitoriaComRetryNaTerceiraTentativa() {
        when(catalogoRestClient.buscarPorId(1L))
                .thenThrow(new CatalogoRemotoException("503 transitório 1"))
                .thenThrow(new CatalogoRemotoException("503 transitório 2"))
                .thenReturn(new JogoCatalogoDTO(1L, "Hollow Knight", "PC", "Metroidvania", 27));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 1}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(201)
                .body("titulo", equalTo("Hollow Knight"));

        verify(catalogoRestClient, times(3)).buscarPorId(1L);
    }

    @Test
    void deveAcionarTimeoutRetryEFallback503SemPersistirNaEscrita() {
        when(catalogoRestClient.buscarPorId(2L)).thenAnswer(invocation -> {
            Thread.sleep(500);
            return new JogoCatalogoDTO(2L, "The Witcher 3", "PC", "RPG", 103);
        });

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 2}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(503)
                .body("codigo", equalTo("CATALOGO_INDISPONIVEL"));

        assertEquals(0, itemBacklogRepository.count());
        verify(catalogoRestClient, times(3)).buscarPorId(2L);
    }

    @Test
    void deveDegradarComSnapshotLocalNoGetDetalhesQuandoCatalogoFalhar() {
        when(catalogoRestClient.buscarPorId(1L))
                .thenReturn(new JogoCatalogoDTO(1L, "Hollow Knight", "PC", "Metroidvania", 27))
                .thenThrow(new CatalogoRemotoException("Catálogo fora do ar"));

        Long itemId = given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 1}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/joao/backlog/{id}/detalhes", itemId)
                .then()
                .statusCode(200)
                .body("titulo", equalTo("Hollow Knight"))
                .body("duracaoMediaHoras", equalTo(27))
                .body("catalogoDisponivel", equalTo(false))
                .body("plataforma", nullValue())
                .body("genero", nullValue())
                .body("aviso", notNullValue());
    }

    @Test
    void deveAbrirCircuitBreakerERecusarNovasChamadasSemTocarNoCliente() {
        when(catalogoRestClient.buscarPorId(1L))
                .thenReturn(new JogoCatalogoDTO(1L, "Hollow Knight", "PC", "Metroidvania", 27));
        Long itemId = given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 1}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        // Zera a janela após o cadastro inicial para que apenas as falhas abaixo contem para o threshold=6
        cbMaintenance.resetAll();

        when(catalogoRestClient.buscarPorId(4L))
                .thenThrow(new CatalogoRemotoException("503 contínuo"));
        when(catalogoRestClient.buscarPorId(1L))
                .thenThrow(new CatalogoRemotoException("503 contínuo"));

        // Cada requisição executa 1 tentativa + 2 retries = 3 chamadas; 2 requisições = 6 chamadas (requestVolumeThreshold=6)
        for (int i = 0; i < 2; i++) {
            given()
                    .auth().preemptive().basic("joao", "joao123")
                    .contentType(ContentType.JSON)
                    .body("{\"jogoId\": 4}")
                    .when()
                    .post("/usuarios/joao/backlog")
                    .then()
                    .statusCode(503)
                    .body("codigo", equalTo("CATALOGO_INDISPONIVEL"));
        }
        verify(catalogoRestClient, times(6)).buscarPorId(4L);

        // Com o disjuntor aberto, a 3ª chamada vai direto ao fallback sem acionar o cliente remoto
        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 4}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(503)
                .body("codigo", equalTo("CATALOGO_INDISPONIVEL"));
        verify(catalogoRestClient, times(6)).buscarPorId(4L);

        // E o endpoint de detalhes continua degradando graciosamente (200 OK com snapshot local)
        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/joao/backlog/{id}/detalhes", itemId)
                .then()
                .statusCode(200)
                .body("catalogoDisponivel", equalTo(false))
                .body("titulo", equalTo("Hollow Knight"));
    }

    @Test
    void devePropagar422Para404DoCatalogoSemRetryESemAbrirDisjuntor() {
        when(catalogoRestClient.buscarPorId(999L))
                .thenThrow(new JogoInexistenteNoCatalogoException(999L));
        when(catalogoRestClient.buscarPorId(1L))
                .thenReturn(new JogoCatalogoDTO(1L, "Hollow Knight", "PC", "Metroidvania", 27));

        for (int i = 0; i < 6; i++) {
            given()
                    .auth().preemptive().basic("joao", "joao123")
                    .contentType(ContentType.JSON)
                    .body("{\"jogoId\": 999}")
                    .when()
                    .post("/usuarios/joao/backlog")
                    .then()
                    .statusCode(422)
                    .body("codigo", equalTo("JOGO_INEXISTENTE"));
        }

        // Sem retry (1 chamada por requisição) e sem contaminar o circuit breaker
        verify(catalogoRestClient, times(6)).buscarPorId(999L);

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 1}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(201);
    }
}

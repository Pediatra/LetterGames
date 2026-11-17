package br.com.lettergames.backlog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import br.com.lettergames.backlog.dominio.CatalogoRequisicaoInvalidaException;
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
class BacklogValidacaoErrosTest {

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
    void deveRetornar400PadronizadoParaValidacaoEJsonInvalido() {
        String corpoValidacao = given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": -10}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("codigo", equalTo("ERRO_VALIDACAO"))
                .body("caminho", equalTo("/usuarios/joao/backlog"))
                .body("timestamp", notNullValue())
                .body("detalhes", not(empty()))
                .extract()
                .asString();
        assertSemDetalhesInternos(corpoValidacao);

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("ERRO_VALIDACAO"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/joao!invalido/backlog")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("ERRO_VALIDACAO"));

        String corpoJson = given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{quebrado")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("codigo", equalTo("JSON_INVALIDO"))
                .extract()
                .asString();
        assertSemDetalhesInternos(corpoJson);

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"status\": \"NAO_EXISTE\"}")
                .when()
                .patch("/usuarios/joao/backlog/1")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("JSON_INVALIDO"));
    }

    @Test
    void deveRetornar404E409E422ParaRegrasDeDominioDoBacklog() {
        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/joao/backlog/99999")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("codigo", equalTo("ITEM_NAO_ENCONTRADO"));

        when(catalogoRestClient.buscarPorId(999L))
                .thenThrow(new JogoInexistenteNoCatalogoException(999L));
        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 999}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(422)
                .body("status", equalTo(422))
                .body("codigo", equalTo("JOGO_INEXISTENTE"));

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

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 1}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("codigo", equalTo("JOGO_JA_NO_BACKLOG"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ZERADO\"}")
                .when()
                .patch("/usuarios/joao/backlog/{id}", itemId)
                .then()
                .statusCode(422)
                .body("status", equalTo(422))
                .body("codigo", equalTo("TRANSICAO_INVALIDA"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"status\": \"QUERO_JOGAR\"}")
                .when()
                .patch("/usuarios/joao/backlog/{id}", itemId)
                .then()
                .statusCode(422)
                .body("codigo", equalTo("TRANSICAO_INVALIDA"));
    }

    @Test
    void deveRetornar422QuandoLimiteDeJogosJogandoForAtingido() {
        for (long id = 1; id <= 4; id++) {
            when(catalogoRestClient.buscarPorId(id))
                    .thenReturn(new JogoCatalogoDTO(id, "Jogo " + id, "PC", "RPG", 20));
            Long itemId = given()
                    .auth().preemptive().basic("joao", "joao123")
                    .contentType(ContentType.JSON)
                    .body("{\"jogoId\": " + id + "}")
                    .when()
                    .post("/usuarios/joao/backlog")
                    .then()
                    .statusCode(201)
                    .extract()
                    .jsonPath()
                    .getLong("id");

            int esperado = id <= 3 ? 200 : 422;
            var resp = given()
                    .auth().preemptive().basic("joao", "joao123")
                    .contentType(ContentType.JSON)
                    .body("{\"status\": \"JOGANDO\"}")
                    .when()
                    .patch("/usuarios/joao/backlog/{id}", itemId)
                    .then()
                    .statusCode(esperado);

            if (esperado == 422) {
                resp.body("codigo", equalTo("LIMITE_JOGANDO_ATINGIDO"));
            }
        }
    }

    @Test
    void deveRetornar502E503ParaErrosDeIntegracaoComCatalogo() {
        when(catalogoRestClient.buscarPorId(77L))
                .thenThrow(new CatalogoRequisicaoInvalidaException(400));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 77}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(502)
                .body("status", equalTo(502))
                .body("codigo", equalTo("ERRO_INTEGRACAO_CATALOGO"));

        when(catalogoRestClient.buscarPorId(88L))
                .thenThrow(new CatalogoRemotoException("503"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 88}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(503)
                .body("status", equalTo(503))
                .body("codigo", equalTo("CATALOGO_INDISPONIVEL"));
    }

    private static void assertSemDetalhesInternos(String corpo) {
        assertFalse(corpo.contains("Exception"));
        assertFalse(corpo.contains("org.hibernate"));
        assertFalse(corpo.contains("br.com.lettergames"));
        assertFalse(corpo.contains("SELECT "));
    }
}

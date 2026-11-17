package br.com.lettergames.backlog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import br.com.lettergames.backlog.dominio.ItemBacklogRepository;
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
class BacklogResourceTest {

    @InjectMock
    @RestClient
    CatalogoRestClient catalogoRestClient;

    @Inject
    ItemBacklogRepository itemBacklogRepository;

    @Inject
    CircuitBreakerMaintenance cbMaintenance;

    @BeforeEach
    void limparEstado() {
        QuarkusTransaction.requiringNew().run(() -> itemBacklogRepository.deleteAll());
        cbMaintenance.resetAll();
    }

    @Test
    void deveExecutarCicloCompletoDoBacklogComStatusHttpCoerentes() {
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
                .header("Location", containsString("/usuarios/joao/backlog/"))
                .body("id", notNullValue())
                .body("usuarioId", equalTo("joao"))
                .body("jogoId", equalTo(1))
                .body("titulo", equalTo("Hollow Knight"))
                .body("duracaoMediaHoras", equalTo(27))
                .body("status", equalTo("QUERO_JOGAR"))
                .extract()
                .jsonPath()
                .getLong("id");

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/joao/backlog/{id}", itemId)
                .then()
                .statusCode(200)
                .body("titulo", equalTo("Hollow Knight"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"status\": \"JOGANDO\"}")
                .when()
                .patch("/usuarios/joao/backlog/{id}", itemId)
                .then()
                .statusCode(200)
                .body("status", equalTo("JOGANDO"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .queryParam("status", "JOGANDO")
                .when()
                .get("/usuarios/joao/backlog")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(itemId.intValue()));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ZERADO\"}")
                .when()
                .patch("/usuarios/joao/backlog/{id}", itemId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ZERADO"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/joao/backlog/resumo")
                .then()
                .statusCode(200)
                .body("total", equalTo(1))
                .body("horasPendentes", equalTo(0))
                .body("horasZeradas", equalTo(27))
                .body("percentualZerado", equalTo(100.0f));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/joao/backlog/{id}/detalhes", itemId)
                .then()
                .statusCode(200)
                .body("catalogoDisponivel", equalTo(true))
                .body("plataforma", equalTo("PC"))
                .body("genero", equalTo("Metroidvania"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .delete("/usuarios/joao/backlog/{id}", itemId)
                .then()
                .statusCode(204);

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/joao/backlog/{id}", itemId)
                .then()
                .statusCode(404)
                .body("codigo", equalTo("ITEM_NAO_ENCONTRADO"));
    }

    @Test
    void deveExporEstatisticasGlobaisParaAdminEContratoOpenApi() {
        when(catalogoRestClient.buscarPorId(1L))
                .thenReturn(new JogoCatalogoDTO(1L, "Hollow Knight", "PC", "Metroidvania", 27));
        when(catalogoRestClient.buscarPorId(3L))
                .thenReturn(new JogoCatalogoDTO(3L, "Celeste", "Switch", "Plataforma", 12));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 1}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(201);

        given()
                .auth().preemptive().basic("maria", "maria123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 3}")
                .when()
                .post("/usuarios/maria/backlog")
                .then()
                .statusCode(201);

        given()
                .auth().preemptive().basic("admin", "admin123")
                .when()
                .get("/admin/backlogs/estatisticas")
                .then()
                .statusCode(200)
                .body("totalJogadoresComBacklog", equalTo(2))
                .body("totalItens", equalTo(2))
                .body("horasPendentes", equalTo(39));

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.'/usuarios/{usuarioId}/backlog'", notNullValue())
                .body("paths.'/admin/backlogs/estatisticas'", notNullValue())
                .body("components.schemas.ErroPadrao", notNullValue())
                .body("components.securitySchemes.basicAuth.scheme", equalTo("basic"));
    }
}

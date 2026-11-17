package br.com.lettergames.backlog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import br.com.lettergames.backlog.dominio.ItemBacklogRepository;
import br.com.lettergames.backlog.integracao.CatalogoRemotoException;
import br.com.lettergames.backlog.integracao.CatalogoRestClient;
import br.com.lettergames.backlog.integracao.JogoCatalogoDTO;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BacklogObservabilidadeConfigTest {

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
    void deveResponderUpEmHealthLiveEReadySemAutenticacao() {
        given()
                .when()
                .get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("backlog-service-vivo"));

        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("backlog-banco-h2"))
                .body("checks.find { it.name == 'backlog-banco-h2' }.data.limiteJogando", equalTo(3));
    }

    @Test
    void deveIncrementarMetricasPropriasEmMetrics() {
        String antes = given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("lettergames_backlog_itens_adicionados_total"))
                .body(containsString("lettergames_backlog_jogos_zerados_total"))
                .body(containsString("lettergames_backlog_catalogo_falhas_total"))
                .extract()
                .asString();

        double adicionadosAntes = lerMetrica(antes, "lettergames_backlog_itens_adicionados_total");
        double zeradosAntes = lerMetrica(antes, "lettergames_backlog_jogos_zerados_total");
        double falhasAntes = lerMetrica(antes, "lettergames_backlog_catalogo_falhas_total");

        when(catalogoRestClient.buscarPorId(1L))
                .thenReturn(new JogoCatalogoDTO(1L, "Hollow Knight", "PC", "Metroidvania", 27));
        when(catalogoRestClient.buscarPorId(99L))
                .thenThrow(new CatalogoRemotoException("503"));

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
                .body("{\"status\": \"JOGANDO\"}")
                .when()
                .patch("/usuarios/joao/backlog/{id}", itemId)
                .then()
                .statusCode(200);

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ZERADO\"}")
                .when()
                .patch("/usuarios/joao/backlog/{id}", itemId)
                .then()
                .statusCode(200);

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 99}")
                .when()
                .post("/usuarios/joao/backlog")
                .then()
                .statusCode(503);

        String depois = given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertTrue(lerMetrica(depois, "lettergames_backlog_itens_adicionados_total") >= adicionadosAntes + 1.0);
        assertTrue(lerMetrica(depois, "lettergames_backlog_jogos_zerados_total") >= zeradosAntes + 1.0);
        assertTrue(lerMetrica(depois, "lettergames_backlog_catalogo_falhas_total") >= falhasAntes + 1.0);
    }

    private static double lerMetrica(String textoMetrics, String nome) {
        Pattern pattern = Pattern.compile("(?m)^" + Pattern.quote(nome) + "(?:\\{[^}]*\\})?\\s+([0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)$");
        Matcher matcher = pattern.matcher(textoMetrics);
        if (!matcher.find()) {
            return 0.0;
        }
        return Double.parseDouble(matcher.group(1));
    }
}

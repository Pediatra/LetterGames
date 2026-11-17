package br.com.lettergames.catalogo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CatalogoObservabilidadeConfigTest {

    @Test
    void deveResponderUpEmHealthLiveEReadySemAutenticacao() {
        given()
                .when()
                .get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("catalogo-service-vivo"));

        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("catalogo-banco-h2"))
                .body("checks.find { it.name == 'catalogo-banco-h2' }.data.duracaoMaximaHoras", equalTo(500));
    }

    @Test
    void deveIncrementarMetricasPropriasEmMetrics() {
        String antes = given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("lettergames_catalogo_jogos_criados_total"))
                .body(containsString("lettergames_catalogo_consultas_jogo_total"))
                .extract()
                .asString();

        double criadosAntes = lerMetrica(antes, "lettergames_catalogo_jogos_criados_total");
        double consultasAntes = lerMetrica(antes, "lettergames_catalogo_consultas_jogo_total");

        String novoJogo = """
                {
                  "titulo": "Tunic Métrica",
                  "plataforma": "PC",
                  "genero": "Aventura",
                  "duracaoMediaHoras": 15
                }
                """;

        Long id = given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body(novoJogo)
                .when()
                .post("/jogos")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/jogos/{id}", id)
                .then()
                .statusCode(200);

        given()
                .auth().preemptive().basic("admin", "admin123")
                .when()
                .delete("/jogos/{id}", id)
                .then()
                .statusCode(204);

        String depois = given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        double criadosDepois = lerMetrica(depois, "lettergames_catalogo_jogos_criados_total");
        double consultasDepois = lerMetrica(depois, "lettergames_catalogo_consultas_jogo_total");

        assertTrue(criadosDepois >= criadosAntes + 1.0);
        assertTrue(consultasDepois >= consultasAntes + 1.0);
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

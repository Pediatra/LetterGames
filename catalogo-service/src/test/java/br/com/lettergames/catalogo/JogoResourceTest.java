package br.com.lettergames.catalogo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JogoResourceTest {

    @Test
    void deveCriarJogoComStatus201ELocation() {
        String payload = """
                {
                  "titulo": "Disco Elysium Teste REST",
                  "plataforma": "PC",
                  "genero": "RPG",
                  "duracaoMediaHoras": 32
                }
                """;

        Long id = given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/jogos")
                .then()
                .statusCode(201)
                .header("Location", containsString("/jogos/"))
                .body("id", notNullValue())
                .body("titulo", equalTo("Disco Elysium Teste REST"))
                .body("plataforma", equalTo("PC"))
                .body("genero", equalTo("RPG"))
                .body("duracaoMediaHoras", equalTo(32))
                .extract()
                .jsonPath()
                .getLong("id");

        given()
                .auth().preemptive().basic("admin", "admin123")
                .when()
                .delete("/jogos/{id}", id)
                .then()
                .statusCode(204);
    }

    @Test
    void deveListarJogosComFiltrosPorPlataformaGeneroETermo() {
        given()
                .auth().preemptive().basic("joao", "joao123")
                .queryParam("plataforma", "pc")
                .queryParam("genero", "rpg")
                .queryParam("termo", "witcher")
                .when()
                .get("/jogos")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].titulo", equalTo("The Witcher 3"))
                .body("titulo", not(hasItem("Hollow Knight")));
    }

    @Test
    void deveBuscarJogoPorIdComStatus200() {
        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/jogos/1")
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("titulo", equalTo("Hollow Knight"))
                .body("duracaoMediaHoras", equalTo(27));
    }

    @Test
    void deveAtualizarERemoverJogoComStatus200E204() {
        String criacao = """
                {
                  "titulo": "Outer Wilds Teste Update",
                  "plataforma": "PC",
                  "genero": "Aventura",
                  "duracaoMediaHoras": 18
                }
                """;

        Long id = given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body(criacao)
                .when()
                .post("/jogos")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        String atualizacao = """
                {
                  "titulo": "Outer Wilds: Echoes",
                  "plataforma": "Switch",
                  "genero": "Exploração",
                  "duracaoMediaHoras": 24
                }
                """;

        given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body(atualizacao)
                .when()
                .put("/jogos/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("titulo", equalTo("Outer Wilds: Echoes"))
                .body("plataforma", equalTo("Switch"))
                .body("duracaoMediaHoras", equalTo(24));

        given()
                .auth().preemptive().basic("admin", "admin123")
                .when()
                .delete("/jogos/{id}", id)
                .then()
                .statusCode(204);

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/jogos/{id}", id)
                .then()
                .statusCode(404)
                .body("codigo", equalTo("JOGO_NAO_ENCONTRADO"));
    }

    @Test
    void deveExporContratoOpenApiComRotasDeJogos() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.'/jogos'", notNullValue())
                .body("paths.'/jogos/{id}'", notNullValue())
                .body("components.schemas.ErroPadrao", notNullValue())
                .body("components.securitySchemes.basicAuth.scheme", equalTo("basic"));
    }
}

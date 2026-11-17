package br.com.lettergames.catalogo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CatalogoValidacaoErrosTest {

    @Test
    void deveRetornar400PadronizadoQuandoCamposForemInvalidos() {
        String payload = """
                {
                  "titulo": " ",
                  "plataforma": "PC",
                  "genero": "RPG",
                  "duracaoMediaHoras": -5
                }
                """;

        String corpo = given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/jogos")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("codigo", equalTo("ERRO_VALIDACAO"))
                .body("caminho", equalTo("/jogos"))
                .body("timestamp", notNullValue())
                .body("detalhes", not(empty()))
                .extract()
                .asString();

        assertSemDetalhesInternos(corpo);
    }

    @Test
    void deveRejeitarCaracteresDeControleENelCom400() {
        String payload = """
                {
                  "titulo": "Jogo\\u0085Injetado",
                  "plataforma": "PC",
                  "genero": "Ação",
                  "duracaoMediaHoras": 10
                }
                """;

        given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/jogos")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("ERRO_VALIDACAO"))
                .body("detalhes[0].campo", equalTo("titulo"));
    }

    @Test
    void deveRetornar400QuandoCorpoForVazioOuJsonMalformado() {
        given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body("")
                .when()
                .post("/jogos")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("codigo", equalTo("ERRO_VALIDACAO"));

        String corpo = given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body("{json-invalido")
                .when()
                .post("/jogos")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("codigo", equalTo("JSON_INVALIDO"))
                .body("caminho", equalTo("/jogos"))
                .extract()
                .asString();

        assertSemDetalhesInternos(corpo);

        given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body("{\"titulo\":\"X\",\"plataforma\":\"PC\",\"genero\":\"RPG\",\"duracaoMediaHoras\":\"abc\"}")
                .when()
                .post("/jogos")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("codigo", equalTo("JSON_INVALIDO"));
    }

    @Test
    void deveRetornar404QuandoJogoNaoExistir() {
        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/jogos/999999")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("codigo", equalTo("JOGO_NAO_ENCONTRADO"))
                .body("mensagem", containsString("999999"))
                .body("caminho", equalTo("/jogos/999999"));
    }

    @Test
    void deveRetornar409QuandoJogoEPlataformaForemDuplicados() {
        String duplicado = """
                {
                  "titulo": "hollow knight",
                  "plataforma": "pc",
                  "genero": "Metroidvania",
                  "duracaoMediaHoras": 27
                }
                """;

        given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body(duplicado)
                .when()
                .post("/jogos")
                .then()
                .statusCode(409)
                .contentType(ContentType.JSON)
                .body("status", equalTo(409))
                .body("codigo", equalTo("JOGO_DUPLICADO"))
                .body("caminho", equalTo("/jogos"));
    }

    @Test
    void deveRetornar422QuandoDuracaoExcederLimiteConfigurado() {
        String longo = """
                {
                  "titulo": "Campanha Infinita",
                  "plataforma": "PC",
                  "genero": "MMO",
                  "duracaoMediaHoras": 501
                }
                """;

        given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body(longo)
                .when()
                .post("/jogos")
                .then()
                .statusCode(422)
                .contentType(ContentType.JSON)
                .body("status", equalTo(422))
                .body("codigo", equalTo("DURACAO_ACIMA_DO_LIMITE"))
                .body("caminho", equalTo("/jogos"));
    }

    private static void assertSemDetalhesInternos(String corpo) {
        assertFalse(corpo.contains("Exception"));
        assertFalse(corpo.contains("org.hibernate"));
        assertFalse(corpo.contains("br.com.lettergames"));
        assertFalse(corpo.contains("SELECT "));
    }
}

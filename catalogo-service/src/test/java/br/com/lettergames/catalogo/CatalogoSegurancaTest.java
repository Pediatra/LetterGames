package br.com.lettergames.catalogo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.lettergames.catalogo.seguranca.UsuarioCatalogo;
import br.com.lettergames.catalogo.seguranca.UsuarioCatalogoRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CatalogoSegurancaTest {

    @Inject
    UsuarioCatalogoRepository usuarioRepository;

    @Test
    void deveRetornar401ComErroPadraoSemCredenciaisOuSenhaIncorreta() {
        given()
                .when()
                .get("/jogos")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .header("WWW-Authenticate", containsString("Basic"))
                .body("status", equalTo(401))
                .body("codigo", equalTo("NAO_AUTENTICADO"))
                .body("caminho", equalTo("/jogos"));

        given()
                .auth().preemptive().basic("joao", "senha-errada")
                .when()
                .get("/jogos")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .header("WWW-Authenticate", containsString("Basic"))
                .body("status", equalTo(401))
                .body("codigo", equalTo("NAO_AUTENTICADO"));
    }

    @Test
    void deveRetornar403QuandoJogadorTentarCadastrarAlterarOuRemoverJogo() {
        String payload = """
                {
                  "titulo": "Sem Permissão",
                  "plataforma": "PC",
                  "genero": "Indie",
                  "duracaoMediaHoras": 5
                }
                """;

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/jogos")
                .then()
                .statusCode(403)
                .contentType(ContentType.JSON)
                .body("status", equalTo(403))
                .body("codigo", equalTo("ACESSO_NEGADO"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .put("/jogos/1")
                .then()
                .statusCode(403)
                .body("codigo", equalTo("ACESSO_NEGADO"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .delete("/jogos/1")
                .then()
                .statusCode(403)
                .body("codigo", equalTo("ACESSO_NEGADO"));
    }

    @Test
    void devePermitirLeituraParaJogadorECriacaoParaAdmin() {
        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/jogos/1")
                .then()
                .statusCode(200);

        String payload = """
                {
                  "titulo": "Celeste Admin Teste",
                  "plataforma": "PC",
                  "genero": "Plataforma",
                  "duracaoMediaHoras": 10
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
    void deveArmazenarSenhasComHashBcryptNoBanco() {
        UsuarioCatalogo joao = usuarioRepository.buscarPorUsername("joao").orElseThrow();
        assertNotEquals("joao123", joao.getSenhaHash());
        assertTrue(joao.getSenhaHash().startsWith("$2"));
    }
}

package br.com.lettergames.backlog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import br.com.lettergames.backlog.dominio.ItemBacklogRepository;
import br.com.lettergames.backlog.integracao.CatalogoRestClient;
import br.com.lettergames.backlog.integracao.JogoCatalogoDTO;
import br.com.lettergames.backlog.seguranca.UsuarioBacklog;
import br.com.lettergames.backlog.seguranca.UsuarioBacklogRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BacklogSegurancaTest {

    @InjectMock
    @RestClient
    CatalogoRestClient catalogoRestClient;

    @Inject
    ItemBacklogRepository itemBacklogRepository;

    @Inject
    UsuarioBacklogRepository usuarioRepository;

    @BeforeEach
    void limpar() {
        QuarkusTransaction.requiringNew().run(() -> itemBacklogRepository.deleteAll());
    }

    @Test
    void deveRetornar401ComErroPadraoSemCredencialOuComSenhaIncorreta() {
        given()
                .when()
                .get("/usuarios/joao/backlog")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .header("WWW-Authenticate", containsString("Basic"))
                .body("status", equalTo(401))
                .body("codigo", equalTo("NAO_AUTENTICADO"));

        given()
                .auth().preemptive().basic("joao", "errada")
                .when()
                .get("/usuarios/joao/backlog")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("status", equalTo(401))
                .body("codigo", equalTo("NAO_AUTENTICADO"));
    }

    @Test
    void deveAplicarRegrasDePapelEPosseDoProprioBacklog() {
        when(catalogoRestClient.buscarPorId(1L))
                .thenReturn(new JogoCatalogoDTO(1L, "Hollow Knight", "PC", "Metroidvania", 27));

        // Maria cria item no próprio backlog
        Long itemMariaId = given()
                .auth().preemptive().basic("maria", "maria123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 1}")
                .when()
                .post("/usuarios/maria/backlog")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        // João não pode ler, adicionar, alterar nem remover o backlog da Maria (regra de posse)
        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/maria/backlog")
                .then()
                .statusCode(403)
                .body("codigo", equalTo("ACESSO_NEGADO"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/maria/backlog/resumo")
                .then()
                .statusCode(403)
                .body("codigo", equalTo("ACESSO_NEGADO"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 1}")
                .when()
                .post("/usuarios/maria/backlog")
                .then()
                .statusCode(403)
                .body("codigo", equalTo("ACESSO_NEGADO"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .contentType(ContentType.JSON)
                .body("{\"status\": \"JOGANDO\"}")
                .when()
                .patch("/usuarios/maria/backlog/{id}", itemMariaId)
                .then()
                .statusCode(403)
                .body("codigo", equalTo("ACESSO_NEGADO"));

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .delete("/usuarios/maria/backlog/{id}", itemMariaId)
                .then()
                .statusCode(403)
                .body("codigo", equalTo("ACESSO_NEGADO"));

        // João acessa o próprio backlog com sucesso e é barrado da rota administrativa
        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/usuarios/joao/backlog")
                .then()
                .statusCode(200);

        given()
                .auth().preemptive().basic("joao", "joao123")
                .when()
                .get("/admin/backlogs/estatisticas")
                .then()
                .statusCode(403)
                .body("codigo", equalTo("ACESSO_NEGADO"));

        // ADMIN pode consultar o backlog de Maria e estatísticas globais, mas não mutar o backlog pessoal dela
        given()
                .auth().preemptive().basic("admin", "admin123")
                .when()
                .get("/usuarios/maria/backlog")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));

        given()
                .auth().preemptive().basic("admin", "admin123")
                .when()
                .get("/admin/backlogs/estatisticas")
                .then()
                .statusCode(200);

        given()
                .auth().preemptive().basic("admin", "admin123")
                .contentType(ContentType.JSON)
                .body("{\"jogoId\": 1}")
                .when()
                .post("/usuarios/maria/backlog")
                .then()
                .statusCode(403)
                .body("codigo", equalTo("ACESSO_NEGADO"));
    }

    @Test
    void deveArmazenarSenhasComHashBcryptNoBanco() {
        UsuarioBacklog joao = usuarioRepository.buscarPorUsername("joao").orElseThrow();
        assertNotEquals("joao123", joao.getSenhaHash());
        assertTrue(joao.getSenhaHash().startsWith("$2"));
    }
}

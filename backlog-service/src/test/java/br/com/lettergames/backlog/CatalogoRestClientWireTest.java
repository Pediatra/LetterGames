package br.com.lettergames.backlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.lettergames.backlog.dominio.CatalogoRequisicaoInvalidaException;
import br.com.lettergames.backlog.dominio.JogoInexistenteNoCatalogoException;
import br.com.lettergames.backlog.integracao.CatalogoRemotoException;
import br.com.lettergames.backlog.integracao.CatalogoRestClient;
import br.com.lettergames.backlog.integracao.JogoCatalogoDTO;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = CatalogoRestClientWireTest.CatalogoFakeServerResource.class, restrictToAnnotatedClass = true)
class CatalogoRestClientWireTest {

    private static final AtomicInteger PROXIMO_STATUS = new AtomicInteger(200);
    private static final AtomicReference<String> ULTIMO_AUTH_HEADER = new AtomicReference<>();

    @RestClient
    CatalogoRestClient catalogoRestClient;

    @Test
    void deveEnviarBasicAuthEDesserializarResposta200PeloFio() {
        PROXIMO_STATUS.set(200);
        ULTIMO_AUTH_HEADER.set(null);

        JogoCatalogoDTO dto = catalogoRestClient.buscarPorId(1L);

        assertEquals(1L, dto.id());
        assertEquals("Hollow Knight", dto.titulo());
        String esperado = "Basic " + Base64.getEncoder()
                .encodeToString("sistema-backlog:sistema123".getBytes(StandardCharsets.UTF_8));
        assertEquals(esperado, ULTIMO_AUTH_HEADER.get());
    }

    @Test
    void deveMapear404E400E503RecebidosNaRede() {
        PROXIMO_STATUS.set(404);
        assertThrows(JogoInexistenteNoCatalogoException.class, () -> catalogoRestClient.buscarPorId(404L));

        PROXIMO_STATUS.set(400);
        assertThrows(CatalogoRequisicaoInvalidaException.class, () -> catalogoRestClient.buscarPorId(400L));

        PROXIMO_STATUS.set(503);
        assertThrows(CatalogoRemotoException.class, () -> catalogoRestClient.buscarPorId(503L));
    }

    public static class CatalogoFakeServerResource implements QuarkusTestResourceLifecycleManager {

        private HttpServer server;

        @Override
        public Map<String, String> start() {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException e) {
                throw new IllegalStateException("Falha ao subir HttpServer falso do catálogo", e);
            }
            server.createContext("/jogos/", exchange -> {
                ULTIMO_AUTH_HEADER.set(exchange.getRequestHeaders().getFirst("Authorization"));
                int status = PROXIMO_STATUS.get();
                byte[] body = status == 200
                        ? """
                          {"id":1,"titulo":"Hollow Knight","plataforma":"PC","genero":"Metroidvania","duracaoMediaHoras":27}
                          """.getBytes(StandardCharsets.UTF_8)
                        : """
                          {"status":%d,"codigo":"ERRO_SIMULADO","mensagem":"simulado"}
                          """.formatted(status).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return Map.of("quarkus.rest-client.catalogo-api.url", "http://127.0.0.1:" + server.getAddress().getPort());
        }

        @Override
        public void stop() {
            if (server != null) {
                server.stop(0);
            }
        }
    }
}

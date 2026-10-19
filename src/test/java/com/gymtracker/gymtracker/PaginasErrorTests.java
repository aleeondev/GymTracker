package com.gymtracker.gymtracker;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Las páginas de error propias (templates/error/*.html) se ven de verdad.
 * Este test arranca un servidor real (RANDOM_PORT): con MockMvc no se ejecuta
 * el paso en el que Spring busca la página de error.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = "spring.datasource.hikari.maximum-pool-size=2")
@ActiveProfiles("local")
class PaginasErrorTests {

    @LocalServerPort
    int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

    private HttpResponse<String> pedir(HttpRequest.Builder peticion) throws Exception {
        // Como un navegador: pedimos HTML (sin esto, Spring responde el error en JSON)
        return cliente.send(peticion.header("Accept", "text/html").build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void error404ConNuestroDiseno() throws Exception {
        HttpResponse<String> r = pedir(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/js/no-existe.js")));
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(r.body()).contains("No encontrado").contains("Volver al panel")
            .doesNotContain("Whitelabel").doesNotContain("Exception");
    }

    @Test
    void error403SinTokenDeSeguridad() throws Exception {
        HttpResponse<String> r = pedir(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/peso/registrar"))
            .POST(HttpRequest.BodyPublishers.ofString("pesoKg=70"))
            .header("Content-Type", "application/x-www-form-urlencoded"));
        assertThat(r.statusCode()).isEqualTo(403);
        assertThat(r.body()).contains("Acceso denegado").contains("Reintentar");
    }
}
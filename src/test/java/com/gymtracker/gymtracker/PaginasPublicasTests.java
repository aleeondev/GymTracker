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
 * Las páginas públicas (landing, login, registro) se pintan sin errores de
 * plantilla y cargan sus scripts desde nuestro servidor (la CSP no deja otros).
 * Misma configuración que PaginasErrorTests: Spring reutiliza el mismo servidor.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = "spring.datasource.hikari.maximum-pool-size=2")
@ActiveProfiles("local")
class PaginasPublicasTests {

    @LocalServerPort
    int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

    private HttpResponse<String> pedir(String ruta) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .header("Accept", "text/html").build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void landingSinSesion() throws Exception {
        HttpResponse<String> r = pedir("/");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body())
            .contains("Evoluciona.").contains("Crear cuenta").contains("Llévala en")
            // GSAP servido desde vendor/ (con huella de caché en el nombre)
            .contains("/js/vendor/gsap-3.13.0").contains("/js/landing-pre")
            // Los iconos nuevos se pintan (no quedan <svg> vacíos)
            .contains("M5 12h14M13 6l6 6-6 6")
            .doesNotContain("Ir a mi panel");
    }
}

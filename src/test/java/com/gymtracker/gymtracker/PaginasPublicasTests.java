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

    @Test
    void privacidadPublicaYEnlazada() throws Exception {
        HttpResponse<String> r = pedir("/privacidad");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("Tus datos son tuyos.").contains("datos de salud")
            .contains("mailto:").contains("aepd.es")
            // Sale de application.properties: la "ó" tiene que llegar bien (codificación UTF-8)
            .contains("Alejandro León").doesNotContain("PENDIENTE");
        assertThat(pedir("/registro").body()).contains("href=\"/privacidad\"");
        assertThat(pedir("/").body()).contains("href=\"/privacidad\"");
    }

    @Test
    void pantallasDeAccesoConPanelDeMarca() throws Exception {
        for (String ruta : new String[] { "/login", "/registro", "/recuperar", "/nueva-contrasena", "/enlace-error" }) {
            HttpResponse<String> r = pedir(ruta);
            assertThat(r.statusCode()).as(ruta).isEqualTo(200);
            assertThat(r.body()).as(ruta)
                .contains("class=\"auth-escena\"").contains("auth-barra").contains("¡Nuevo récord!")
                .doesNotContain("minlength=\"6\"");
        }
        assertThat(pedir("/login").body()).contains("Tu próxima serie te está esperando.").contains("data-saludo");
        assertThat(pedir("/registro").body()).contains("2 · Confirma tu email").contains("minlength=\"8\"");
    }
}

package com.gymtracker.gymtracker;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.repository.EjercicioRepository;

import jakarta.servlet.http.Cookie;

/**
 * El token CSRF no debe cambiar mientras navegas.
 *
 * Error real (17/10/2026): con la pantalla de entrenar abierta, cualquier otra
 * petición (otra pestaña, o el navegador pidiendo /sw.js o el manifest de la app
 * instalable) cambiaba el token, y al guardar el entrenamiento daba 403.
 *
 * Aquí el login va por la cookie gt_session y nuestro SupabaseCookieFilter, igual
 * que en el navegador. Solo sustituimos la comprobación de la firma de Supabase.
 */
// Pocas conexiones: Supabase gratis admite 15 a la vez y la app abierta ya usa hasta 10
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=2")
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class SesionCsrfTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    EjercicioRepository ejercicioRepository;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void elTokenNoCambiaConElFormularioAbierto() throws Exception {
        UUID yo = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(Jwt.withTokenValue("x").header("alg", "none")
            .subject(yo.toString()).claim("user_metadata", Map.of("nombre", "Prueba")).build());
        Cookie sesion = new Cookie("gt_session", "token-de-prueba");

        // 1) Abres la pantalla de entrenar: el servidor te da el token (cookie + campo oculto)
        MvcResult pantalla = mvc.perform(get("/entrenamiento/nuevo").cookie(sesion)).andReturn();
        Cookie xsrf = pantalla.getResponse().getCookie("XSRF-TOKEN");
        String token = pantalla.getResponse().getContentAsString()
            .split("name=\"_csrf\" value=\"", 2)[1].split("\"", 2)[0];

        // 2) Mientras entrenas, el navegador pide otras cosas: nadie toca la cookie
        for (String url : new String[] { "/dashboard", "/historial", "/sw.js", "/manifest.webmanifest" }) {
            mvc.perform(get(url).cookie(sesion, xsrf)).andExpect(header().doesNotExist("Set-Cookie"));
        }

        // 3) Guardas con el token de la pantalla que abriste al principio: funciona
        long ejercicioId = ejercicioRepository.findVisibles(yo).get(0).getId();
        mvc.perform(post("/entrenamiento/guardar").cookie(sesion, xsrf).param("_csrf", token)
                .param("series[0].bloque", "0").param("series[0].ejercicioId", String.valueOf(ejercicioId))
                .param("series[0].repeticiones", "8").param("series[0].peso", "70"))
            .andExpect(redirectedUrlPattern("/historial/*?guardado"));
    }
}
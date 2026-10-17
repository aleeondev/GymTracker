package com.gymtracker.gymtracker;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.repository.EjercicioRepository;

/**
 * Recorre la app como lo haría un usuario: crear rutina, entrenar, ver
 * historial y progreso. Usa la base de datos del perfil "local" pero todo
 * se deshace al terminar (@Transactional = rollback), así que no deja datos.
 *
 * El login se simula con un token falso (jwt()), sin pasar por Supabase.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class FlujoUsuarioTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    EjercicioRepository ejercicioRepository;

    /**
     * Usuario simulado. (No usamos jwt() de spring-security-test porque ese
     * atajo se salta la protección CSRF, y queremos comprobarla.)
     */
    private static RequestPostProcessor usuario(UUID id, String nombre) {
        Jwt token = Jwt.withTokenValue("token-de-prueba")
            .header("alg", "none")
            .subject(id.toString())
            .claim("user_metadata", Map.of("nombre", nombre))
            .build();
        return authentication(new JwtAuthenticationToken(token, List.of()));
    }

    @Test
    void paginasPublicasYProteccion() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")));
        mvc.perform(get("/registro")).andExpect(status().isOk());

        // Sin sesión: al login, recordando a dónde ibas
        mvc.perform(get("/rutinas")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?next=%2Frutinas"));

        // POST sin token CSRF: rechazado
        mvc.perform(post("/rutinas/guardar").param("nombre", "X").with(usuario(UUID.randomUUID(), "A")))
            .andExpect(status().isForbidden());
    }

    @Test
    void flujoCompleto() throws Exception {
        UUID yo = UUID.randomUUID();
        RequestPostProcessor auth = usuario(yo, "Prueba");

        mvc.perform(get("/dashboard").with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Hola, Prueba.")));

        // Crear rutina
        String urlRutina = mvc.perform(post("/rutinas/guardar").with(auth).with(csrf()).param("nombre", "Día de prueba"))
            .andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        long rutinaId = Long.parseLong(urlRutina.substring(urlRutina.lastIndexOf('/') + 1));

        long ejercicioId = ejercicioRepository.findVisibles(yo).get(0).getId();
        mvc.perform(post("/rutinas/" + rutinaId + "/ejercicios/guardar").with(auth).with(csrf())
                .param("ejercicioId", String.valueOf(ejercicioId))
                .param("series", "3").param("repeticiones", "10").param("pesoObjetivo", "50").param("descanso", "90"))
            .andExpect(redirectedUrl("/rutinas/" + rutinaId));

        for (String url : new String[] { "/rutinas", "/rutinas/" + rutinaId, "/rutinas/editar/" + rutinaId,
                "/rutinas/nuevo", "/rutinas/" + rutinaId + "/ejercicios/nuevo", "/ejercicios", "/ejercicios?grupo=Pecho",
                "/ejercicios/nuevo", "/entrenamiento", "/entrenamiento/nuevo", "/entrenamiento/nuevo?rutina=" + rutinaId,
                "/historial", "/progreso" }) {
            mvc.perform(get(url).with(auth)).andExpect(status().isOk());
        }

        // Dos entrenamientos (hace una semana y hoy) para que haya progreso
        String primero = guardarEntreno(auth, rutinaId, ejercicioId, LocalDate.now().minusDays(7), "50", "10");
        String segundo = guardarEntreno(auth, rutinaId, ejercicioId, LocalDate.now(), "55", "10");

        mvc.perform(get(segundo).with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Récord")));
        String idSegundo = segundo.replaceAll("\\D+(\\d+).*", "$1");
        for (String url : new String[] { primero, "/historial", "/historial/" + idSegundo + "/editar",
                "/progreso", "/progreso/" + ejercicioId, "/dashboard" }) {
            mvc.perform(get(url).with(auth)).andExpect(status().isOk());
        }

        // Otro usuario no puede ver ni tocar lo mío
        RequestPostProcessor otro = usuario(UUID.randomUUID(), "Otro");
        mvc.perform(get("/rutinas/" + rutinaId).with(otro)).andExpect(status().isNotFound());
        mvc.perform(get("/historial/" + idSegundo).with(otro)).andExpect(status().isNotFound());
        mvc.perform(post("/rutinas/eliminar/" + rutinaId).with(otro).with(csrf())).andExpect(status().isNotFound());

        // Borrar la rutina conserva el historial
        mvc.perform(post("/rutinas/eliminar/" + rutinaId).with(auth).with(csrf())).andExpect(redirectedUrl("/rutinas"));
        mvc.perform(get("/historial/" + idSegundo).with(auth)).andExpect(status().isOk());
    }

    private String guardarEntreno(RequestPostProcessor auth, long rutinaId, long ejercicioId, LocalDate fecha,
                                  String peso, String reps) throws Exception {
        return mvc.perform(post("/entrenamiento/guardar").with(auth).with(csrf())
                .param("rutinaId", String.valueOf(rutinaId))
                .param("fecha", fecha.toString())
                .param("duracionMinutos", "45")
                .param("series[0].bloque", "0").param("series[0].ejercicioId", String.valueOf(ejercicioId))
                .param("series[0].repeticiones", reps).param("series[0].peso", peso)
                .param("series[1].bloque", "0").param("series[1].ejercicioId", String.valueOf(ejercicioId))
                .param("series[1].repeticiones", reps).param("series[1].peso", peso)
                .param("series[2].bloque", "0").param("series[2].ejercicioId", String.valueOf(ejercicioId))
                .param("series[2].repeticiones", "").param("series[2].peso", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/historial/*?guardado"))
            .andReturn().getResponse().getRedirectedUrl();
    }
}
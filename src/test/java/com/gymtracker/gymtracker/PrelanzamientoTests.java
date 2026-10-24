package com.gymtracker.gymtracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.model.Interesado;
import com.gymtracker.gymtracker.repository.InteresadoRepository;

/**
 * Web de prelanzamiento (/pronto): la página es pública y la lista de
 * acceso anticipado guarda solo emails válidos, con permiso y sin duplicados.
 * Todo se deshace al terminar (@Transactional = rollback).
 */
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=2")
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class PrelanzamientoTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    InteresadoRepository repository;

    /** Email único por ejecución, para no chocar con datos reales. */
    private static String emailNuevo() {
        return "prueba-" + UUID.randomUUID() + "@example.com";
    }

    /** Envío como lo hace pronto.js. Cada test usa su propia IP para no tocar el límite de los demás. */
    private static MockHttpServletRequestBuilder apuntar(String ip) {
        return post("/pronto/apuntarme").with(csrf())
            .with(peticion -> { peticion.setRemoteAddr(ip); return peticion; })
            .accept(MediaType.APPLICATION_JSON);
    }

    @Test
    void paginaPublicaConSuDiseno() throws Exception {
        mvc.perform(get("/pronto")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Fitness Challenge")))
            .andExpect(content().string(containsString("/css/pronto-")))
            .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void apuntarseGuardaElEmailNormalizado() throws Exception {
        String email = emailNuevo();
        mvc.perform(apuntar("10.0.0.1").param("email", "  " + email.toUpperCase() + " ")
                .param("nombre", "  Ana   María ").param("puntuacion", "850").param("consentimiento", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value(containsString("Estás dentro")));

        Interesado guardado = repository.findAll().stream()
            .filter(i -> i.getEmail().equals(email)).findFirst().orElseThrow();
        assertThat(guardado.getNombre()).isEqualTo("Ana María");
        assertThat(guardado.getPuntuacion()).isEqualTo(850);
        assertThat(guardado.getConsentimientoEn()).isNotNull();
        assertThat(guardado.getTokenBaja()).isNotNull();
    }

    @Test
    void sinPermisoNoSeGuarda() throws Exception {
        String email = emailNuevo();
        mvc.perform(apuntar("10.0.0.2").param("email", email))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("permiso")));
        assertThat(repository.existsByEmail(email)).isFalse();
    }

    @Test
    void datosNoValidosSeRechazan() throws Exception {
        mvc.perform(apuntar("10.0.0.3").param("email", "esto-no-es-un-email").param("consentimiento", "true"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("email")));
        // Puntuación imposible (el máximo del reto es 2000)
        mvc.perform(apuntar("10.0.0.3").param("email", emailNuevo()).param("consentimiento", "true").param("puntuacion", "99999"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void repetirEmailNoDuplicaNiLoDelata() throws Exception {
        String email = emailNuevo();
        mvc.perform(apuntar("10.0.0.4").param("email", email).param("consentimiento", "true")).andExpect(status().isOk());
        long antes = repository.count();
        // Misma respuesta: así nadie puede averiguar si un email ya estaba apuntado
        mvc.perform(apuntar("10.0.0.4").param("email", email).param("consentimiento", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value(containsString("Estás dentro")));
        assertThat(repository.count()).isEqualTo(antes);
    }

    @Test
    void losBotsNoEntran() throws Exception {
        String email = emailNuevo();
        mvc.perform(apuntar("10.0.0.5").param("email", email).param("consentimiento", "true").param("web", "http://spam.example"))
            .andExpect(status().isOk());
        assertThat(repository.existsByEmail(email)).isFalse();
    }

    @Test
    void sinTokenCsrfRechazado() throws Exception {
        mvc.perform(post("/pronto/apuntarme").param("email", emailNuevo()).param("consentimiento", "true"))
            .andExpect(status().isForbidden());
    }

    @Test
    void limiteDePeticionesPorIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(apuntar("10.0.0.6").param("email", emailNuevo()).param("consentimiento", "true"))
                .andExpect(status().isOk());
        }
        mvc.perform(apuntar("10.0.0.6").param("email", emailNuevo()).param("consentimiento", "true"))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void sinJavaScriptVuelveALaPagina() throws Exception {
        String email = emailNuevo();
        mvc.perform(post("/pronto/apuntarme").with(csrf())
                .with(peticion -> { peticion.setRemoteAddr("10.0.0.7"); return peticion; })
                .accept(MediaType.TEXT_HTML).param("email", email).param("consentimiento", "true"))
            .andExpect(redirectedUrl("/pronto?apuntado#apuntate"));
        assertThat(repository.existsByEmail(email)).isTrue();
    }

    @Test
    void darseDeBajaBorraLosDatos() throws Exception {
        String email = emailNuevo();
        mvc.perform(apuntar("10.0.0.8").param("email", email).param("consentimiento", "true")).andExpect(status().isOk());
        Interesado interesado = repository.findAll().stream()
            .filter(i -> i.getEmail().equals(email)).findFirst().orElseThrow();
        String token = interesado.getTokenBaja().toString();

        // Abrir el enlace solo pregunta (los programas de correo abren enlaces solos)
        mvc.perform(get("/pronto/baja").param("token", token))
            .andExpect(status().isOk()).andExpect(content().string(containsString("¿Te vas?")));
        assertThat(repository.existsByEmail(email)).isTrue();

        mvc.perform(post("/pronto/baja").with(csrf()).param("token", token))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Baja completada")));
        assertThat(repository.existsByEmail(email)).isFalse();

        // Enlace inventado o ya usado
        mvc.perform(get("/pronto/baja").param("token", "no-es-un-token"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Enlace no válido")));
        mvc.perform(get("/pronto/baja").param("token", token))
            .andExpect(content().string(containsString("Enlace no válido")));
    }
}
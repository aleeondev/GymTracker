package com.gymtracker.gymtracker;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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
// Pocas conexiones: Supabase gratis admite 15 a la vez y la app abierta ya usa hasta 10
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=2")
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

        // CSS y JS con "huella" automática en el nombre (app.css -> app-<hash>.css)
        mvc.perform(get("/login")).andExpect(content().string(containsString("/css/auth-")));

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

        // Primera vez: el panel lleva al cuestionario de bienvenida
        mvc.perform(get("/dashboard").with(auth)).andExpect(redirectedUrl("/bienvenida"));
        mvc.perform(get("/bienvenida").with(auth)).andExpect(status().isOk());

        // Cuestionario incompleto: se queda en la página con el aviso (sin perder lo escrito)
        mvc.perform(post("/bienvenida/guardar").with(auth).with(csrf()).param("sexo", "hombre"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("fecha de nacimiento")));

        // Completo: al panel, que ya muestra la tarjeta "Tu objetivo" (IMC 76,5 / 1,78² = 24,1)
        mvc.perform(post("/bienvenida/guardar").with(auth).with(csrf())
                .param("sexo", "hombre").param("fechaNacimiento", "1995-05-10")
                .param("alturaCm", "178").param("pesoKg", "76.5")
                .param("objetivo", "ganar_musculo").param("pesoObjetivoKg", "80").param("experiencia", "de_1_a_3")
                .param("diasSemana", "4").param("duracionSesion", "60").param("lugarEntreno", "gimnasio")
                .param("aceptoDatos", "true"))
            .andExpect(redirectedUrl("/dashboard"));
        mvc.perform(get("/dashboard").with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Hola, Prueba.")))
            .andExpect(content().string(containsString("Ganar músculo")))
            .andExpect(content().string(containsString("24,1")));
        mvc.perform(get("/perfil").with(auth))
            .andExpect(content().string(containsString("TU CUERPO Y TU OBJETIVO")));
        mvc.perform(get("/bienvenida?editar").with(auth)).andExpect(status().isOk());
        // Navegación por áreas: la barra secundaria solo aparece dentro de Entrenamiento
        mvc.perform(get("/rutinas").with(auth)).andExpect(content().string(containsString("class=\"subnav\"")));
        mvc.perform(get("/perfil").with(auth)).andExpect(content().string(org.hamcrest.Matchers.not(containsString("class=\"subnav\""))));
        mvc.perform(get("/nutricion").with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("PRÓXIMAMENTE")));
        mvc.perform(get("/coach").with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Coach IA")));

        // Rutinas recomendadas: 4 días + ganar músculo -> Torso/Pierna con 4 x 10
        mvc.perform(get("/rutinas/recomendadas").with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Recomendado para ti")))
            .andExpect(content().string(containsString("4 series × 10 repeticiones")));
        mvc.perform(post("/rutinas/recomendadas/crear").with(auth).with(csrf()).param("plan", "torso_pierna"))
            .andExpect(redirectedUrl("/rutinas"))
            .andExpect(flash().attribute("exito", containsString("Torso, Pierna")));
        mvc.perform(post("/rutinas/recomendadas/crear").with(auth).with(csrf()).param("plan", "torso_pierna"))
            .andExpect(flash().attribute("exito", containsString("Torso (2)")));
        mvc.perform(post("/rutinas/recomendadas/crear").with(auth).with(csrf()).param("plan", "no_existe"))
            .andExpect(flash().attribute("error", "Ese plan no existe."));

        // Peso corporal: el cuestionario ya apuntó el de hoy (76,5). Añadimos el de ayer y corregimos hoy.
        mvc.perform(post("/peso/registrar").with(auth).with(csrf())
                .param("pesoKg", "77.2").param("fecha", LocalDate.now().minusDays(1).toString()).param("volver", "/perfil"))
            .andExpect(redirectedUrl("/perfil"));
        mvc.perform(post("/peso/registrar").with(auth).with(csrf()).param("pesoKg", "76"))
            .andExpect(redirectedUrl("/dashboard"));
        mvc.perform(post("/peso/registrar").with(auth).with(csrf()).param("pesoKg", "10"))
            .andExpect(flash().attribute("error", containsString("entre 30 y 300")));
        mvc.perform(get("/dashboard").with(auth))
            .andExpect(content().string(containsString("PESO DE HOY ✓")))
            .andExpect(content().string(containsString("76 kg → 80 kg")));
        mvc.perform(get("/perfil").with(auth))
            .andExpect(content().string(containsString("id=\"grafica-peso\"")));

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

        // Con entrenos, el panel muestra la gráfica de volumen semanal
        mvc.perform(get("/dashboard").with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"grafica-volumen\"")));

        // Y en Progreso, la de series por grupo muscular
        mvc.perform(get("/progreso").with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"grafica-grupos\"")))
            .andExpect(content().string(containsString("class=\"mini-grafica\" data-puntos=\"")));
        mvc.perform(get("/progreso/" + ejercicioId).with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"grafica-evolucion\"")));

        // Mis récords: pasó de 50 a 55 kg -> récord batido de +5 kg
        mvc.perform(get("/records").with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Antes: 50 kg")))
            .andExpect(content().string(containsString("+5 kg")));
        String idSegundo = segundo.replaceAll("\\D+(\\d+).*", "$1");
        for (String url : new String[] { primero, "/historial", "/historial/" + idSegundo + "/editar",
                "/progreso", "/progreso/" + ejercicioId, "/dashboard" }) {
            mvc.perform(get(url).with(auth)).andExpect(status().isOk());
        }

        // Perfil: nombre de usuario válido, inválido y repetido por otra persona
        mvc.perform(get("/perfil").with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Cambiar contraseña")));
        mvc.perform(post("/perfil/usuario").with(auth).with(csrf()).param("nombreUsuario", "@Prueba_Gym"))
            .andExpect(redirectedUrl("/perfil"))
            .andExpect(flash().attribute("exito", "Nombre de usuario guardado."));
        mvc.perform(get("/perfil").with(auth)).andExpect(content().string(containsString("@prueba_gym")));
        mvc.perform(post("/perfil/usuario").with(auth).with(csrf()).param("nombreUsuario", "a b"))
            .andExpect(flash().attribute("error", containsString("entre 3 y 20")));
        mvc.perform(post("/perfil/usuario").with(usuario(UUID.randomUUID(), "Otro")).with(csrf()).param("nombreUsuario", "prueba_gym"))
            .andExpect(flash().attribute("error", containsString("ya está cogido")));

        // API del registro (pública): formato no válido, cogido y libre
        mvc.perform(get("/api/usuario-disponible").param("u", "ab")).andExpect(content().string(containsString("formato")));
        mvc.perform(get("/api/usuario-disponible").param("u", "prueba_gym")).andExpect(content().string(containsString("cogido")));
        mvc.perform(get("/api/usuario-disponible").param("u", "nadie_tiene_este")).andExpect(content().string(containsString("libre")));

        // Redirección abierta: "volver" solo acepta rutas de nuestra web
        for (String trampa : new String[] { "//web-falsa.com", "/\t/web-falsa.com", "/" + (char) 92 + "web-falsa.com", "https://web-falsa.com" }) {
            mvc.perform(post("/ejercicios/guardar").with(auth).with(csrf())
                    .param("nombre", "Ejercicio trampa " + trampa.length()).param("grupoMuscular", "Pecho").param("volver", trampa))
                .andExpect(redirectedUrl("/ejercicios"));
        }
        mvc.perform(get("/dashboard").with(auth))
            .andExpect(header().string("Permissions-Policy", containsString("camera=()")));

        // Otro usuario no puede ver ni tocar lo mío
        RequestPostProcessor otro = usuario(UUID.randomUUID(), "Otro");
        mvc.perform(get("/rutinas/" + rutinaId).with(otro)).andExpect(status().isNotFound());
        mvc.perform(get("/historial/" + idSegundo).with(otro)).andExpect(status().isNotFound());
        mvc.perform(post("/rutinas/eliminar/" + rutinaId).with(otro).with(csrf())).andExpect(status().isNotFound());

        // Repetir un entreno: mismos ejercicios, con lo que hiciste ese día como sugerencia
        mvc.perform(get("/entrenamiento").with(auth))
            .andExpect(content().string(containsString("Repite uno reciente")));
        mvc.perform(get("/entrenamiento/nuevo?repetir=" + idSegundo).with(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("referencia")));
        mvc.perform(get("/entrenamiento/nuevo?repetir=" + idSegundo).with(otro)).andExpect(status().isNotFound());

        // Borrar la rutina conserva el historial (y se puede seguir repitiendo)
        mvc.perform(post("/rutinas/eliminar/" + rutinaId).with(auth).with(csrf())).andExpect(redirectedUrl("/rutinas"));
        mvc.perform(get("/historial/" + idSegundo).with(auth)).andExpect(status().isOk());
        mvc.perform(get("/entrenamiento/nuevo?repetir=" + idSegundo).with(auth)).andExpect(status().isOk());
    }

    @Test
    void enviarOpinion() throws Exception {
        RequestPostProcessor auth = usuario(UUID.randomUUID(), "Tester");

        // El botón y su diálogo están en las páginas de la app
        mvc.perform(get("/rutinas").with(auth))
            .andExpect(content().string(containsString("data-abrir-opinion")))
            .andExpect(content().string(containsString("id=\"dialogo-opinion\"")));

        // Bien: responde JSON con el "gracias"
        mvc.perform(post("/opinion").with(auth).with(csrf())
                .param("tipo", "mejora").param("texto", "Pondría un modo oscuro").param("pagina", "/dashboard"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Gracias")));
        // Página de fuera: se guarda igual, pero sin la página (no da error)
        mvc.perform(post("/opinion").with(auth).with(csrf())
                .param("tipo", "gusta").param("texto", "Me encanta").param("pagina", "https://web-falsa.com"))
            .andExpect(status().isOk());

        // Mal: tipo inventado, texto vacío, sin CSRF y sin sesión
        mvc.perform(post("/opinion").with(auth).with(csrf()).param("tipo", "hackeo").param("texto", "Hola hola"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/opinion").with(auth).with(csrf()).param("tipo", "fallo").param("texto", "  "))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("al menos 3")));
        mvc.perform(post("/opinion").with(auth).param("tipo", "fallo").param("texto", "Sin token"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/opinion").with(csrf()).param("tipo", "fallo").param("texto", "Sin sesión"))
            .andExpect(status().is3xxRedirection());
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
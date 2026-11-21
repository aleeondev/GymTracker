package com.gymtracker.gymtracker.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * El "portero": limita cuántas peticiones puede hacer cada persona por minuto.
 *
 * Evita que alguien (o un programa) mande miles de opiniones seguidas, o
 * pregunte por miles de nombres en /api/usuario-disponible para descubrir
 * quién usa la app.
 *
 * Cómo cuenta (técnica "ventana fija"): por cada persona y tipo de petición
 * guarda un contador que se pone a 0 cada minuto. Si el contador pasa del
 * límite, responde 429 "Too Many Requests" y la petición no llega al controlador.
 *
 * "Persona" = el usuario si ha iniciado sesión; si no, su dirección IP.
 *
 * Limitaciones (suficientes para nuestro tamaño):
 * - Los contadores están en memoria: si la app se reinicia, empiezan de 0, y
 *   si algún día hubiera varias copias de la app, cada una contaría por su lado.
 * - La IP se puede falsear en parte detrás de un proxy. Por eso, con sesión
 *   iniciada, contamos por usuario.
 *
 * Se añade a la cadena de seguridad en SecurityConfig (después de
 * SupabaseCookieFilter, para saber ya quién es el usuario).
 */
public class LimitePeticionesFilter extends OncePerRequestFilter {

    private static final long MINUTO_MS = 60_000;

    /** Peticiones permitidas por minuto y persona, según el tipo. */
    private static final int LIMITE_USUARIO_DISPONIBLE = 30;
    private static final int LIMITE_OPINIONES = 5;
    private static final int LIMITE_OTROS_POST = 60;

    /** Contadores: "tipo|persona" -> ventana del minuto actual. */
    private final Map<String, Ventana> contadores = new ConcurrentHashMap<>();
    private volatile long ultimaLimpieza = System.currentTimeMillis();

    private record Ventana(long inicio, AtomicInteger peticiones) {}

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tipo = tipo(request);
        if (tipo == null) {
            chain.doFilter(request, response); // peticiones normales (ver páginas): sin límite
            return;
        }

        long ahora = System.currentTimeMillis();
        limpiarContadoresViejos(ahora);

        String clave = tipo + "|" + persona(request);
        Ventana ventana = contadores.compute(clave, (k, actual) ->
            actual == null || ahora - actual.inicio() >= MINUTO_MS ? new Ventana(ahora, new AtomicInteger()) : actual);

        if (ventana.peticiones().incrementAndGet() > limite(tipo)) {
            rechazar(request, response, ventana, ahora);
            return;
        }
        chain.doFilter(request, response);
    }

    /** Qué tipo de petición es, o null si no la limitamos. */
    private static String tipo(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.equals("/api/usuario-disponible")) {
            return "usuario-disponible";
        }
        if (HttpMethod.POST.matches(request.getMethod())) {
            return uri.equals("/opinion") ? "opinion" : "post";
        }
        return null;
    }

    private static int limite(String tipo) {
        return switch (tipo) {
            case "usuario-disponible" -> LIMITE_USUARIO_DISPONIBLE;
            case "opinion" -> LIMITE_OPINIONES;
            default -> LIMITE_OTROS_POST;
        };
    }

    /** El usuario si ha iniciado sesión; si no, la IP. */
    private static String persona(HttpServletRequest request) {
        return UsuarioActual.autenticado() ? "u:" + UsuarioActual.id() : "ip:" + request.getRemoteAddr();
    }

    /**
     * Responde 429. A las peticiones de JavaScript (fetch) les damos JSON con el
     * mensaje; a los formularios normales, la página de error de la app.
     * La cabecera Retry-After dice cuántos segundos esperar.
     */
    private static void rechazar(HttpServletRequest request, HttpServletResponse response,
                                 Ventana ventana, long ahora) throws IOException {
        long segundos = Math.max(1, (ventana.inicio() + MINUTO_MS - ahora) / 1000);
        response.setHeader("Retry-After", String.valueOf(segundos));

        String accept = request.getHeader("Accept");
        boolean quiereJson = request.getRequestURI().startsWith("/api/")
            || (accept != null && accept.contains("application/json"));
        if (quiereJson) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"Demasiados intentos seguidos. Espera un minuto y vuelve a probar.\"}");
        } else {
            response.sendError(429);
        }
    }

    /** Una vez por minuto, borra los contadores caducados para no llenar la memoria. */
    private void limpiarContadoresViejos(long ahora) {
        if (ahora - ultimaLimpieza < MINUTO_MS) {
            return;
        }
        ultimaLimpieza = ahora;
        contadores.values().removeIf(v -> ahora - v.inicio() >= MINUTO_MS);
    }
}

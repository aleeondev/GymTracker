package com.gymtracker.gymtracker.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad de la aplicación.
 *
 * El login lo hace Supabase Auth en el navegador (supabase-js). Después,
 * auth-session.js guarda el access token (un JWT firmado por Supabase) en
 * la cookie "gt_session". En cada petición, SupabaseCookieFilter lee esa
 * cookie y verifica la firma con las claves públicas de Supabase (JWKS),
 * así que nadie puede falsificar un usuario. El "sub" del token es el id
 * del usuario en auth.users y es lo que separa los datos de cada persona.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Páginas y recursos accesibles sin iniciar sesión. */
    static final String[] RUTAS_PUBLICAS = {
        "/", "/login", "/registro", "/recuperar", "/nueva-contrasena", "/enlace-error", "/error",
        "/css/**", "/js/**", "/img/**", "/favicon.ico",
        // App instalable (PWA): el navegador los pide sin sesión
        "/manifest.webmanifest", "/sw.js", "/offline.html",
        // Registro: comprobar si un nombre de usuario está libre
        "/api/usuario-disponible",
        // Web de prelanzamiento: pantalla de carga, Fitness Challenge y lista de espera
        "/pronto", "/pronto/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                            @Value("${supabase.url}") String supabaseUrl) throws Exception {
        http
            .addFilterBefore(new SupabaseCookieFilter(jwtDecoder), BasicAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(RUTAS_PUBLICAS).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(SecurityConfig::redirigirALogin))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Protección CSRF: cada formulario POST lleva un token oculto (_csrf) que
            // otra web no puede conocer. Se guarda en cookie (no en sesión) para que un
            // entrenamiento largo no falle al guardar por haber caducado la sesión.
            .csrf(csrf -> csrf
                .csrfTokenRepository(new CookieCsrfTokenRepository())
                // Por defecto Spring cambia el token CSRF en cada inicio de sesión. Como no
                // usamos sesiones (STATELESS), Spring cree que CADA petición es un inicio de
                // sesión y lo cambiaba sin parar: un formulario abierto (el de entrenar) se
                // quedaba con un token viejo y al guardar daba 403 si mientras tanto el
                // navegador pedía otra página (otra pestaña, /sw.js, el manifest...).
                // El token sigue siendo secreto y propio de tu navegador; solo deja de rotar.
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
            .headers(h -> h
                // CSP: el navegador solo ejecuta scripts y estilos de nuestra propia web
                // (más Google Fonts) y solo se conecta a nuestro servidor y a Supabase.
                .contentSecurityPolicy(csp -> csp.policyDirectives(String.join("; ",
                    "default-src 'self'",
                    "script-src 'self'",
                    "style-src 'self' https://fonts.googleapis.com",
                    "font-src 'self' https://fonts.gstatic.com",
                    "img-src 'self' data:",
                    "connect-src 'self' " + supabaseUrl,
                    "frame-ancestors 'none'",
                    "form-action 'self'",
                    "base-uri 'self'",
                    "object-src 'none'")))
                .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // La app no usa cámara, micrófono, ubicación ni pagos: los bloqueamos
                // (si algún día hay "foto del plato", se permitirá la cámara aquí).
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                    "camera=(), microphone=(), geolocation=(), payment=(), usb=()")));

        return http.build();
    }

    /** Verifica los JWT de Supabase con sus claves públicas (ES256). */
    @Bean
    JwtDecoder jwtDecoder(@Value("${supabase.url}") String supabaseUrl) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withJwkSetUri(supabaseUrl + "/auth/v1/.well-known/jwks.json")
            .jwsAlgorithm(SignatureAlgorithm.ES256)
            .build();

        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<Object>("aud", aud ->
            aud instanceof Collection<?> lista ? lista.contains("authenticated") : "authenticated".equals(aud));

        // Comprueba caducidad, emisor (nuestro proyecto de Supabase) y audiencia.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(supabaseUrl + "/auth/v1"),
            audiencia));
        return decoder;
    }

    /**
     * Sin sesión válida: las páginas redirigen al login, que renueva el token
     * si Supabase aún tiene la sesión y vuelve a la página pedida (?next=).
     */
    private static void redirigirALogin(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException ex) throws IOException {
        String destino = "/dashboard";
        if (HttpMethod.GET.matches(request.getMethod())) {
            destino = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        }
        response.sendRedirect("/login?next=" + URLEncoder.encode(destino, StandardCharsets.UTF_8));
    }
}
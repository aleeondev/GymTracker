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
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad de la aplicación.
 *
 * El login lo hace Supabase Auth en el navegador (supabase-js). Después,
 * auth-session.js guarda el access token (un JWT firmado por Supabase) en
 * la cookie "gt_session". Spring lo lee en cada petición y verifica la firma
 * con las claves públicas de Supabase (JWKS), así que nadie puede falsificar
 * un usuario. El "sub" del token es el id del usuario en auth.users y es lo
 * que usamos para separar los datos de cada persona.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String SESSION_COOKIE = "gt_session";

    /** Páginas y recursos accesibles sin iniciar sesión. */
    private static final String[] RUTAS_PUBLICAS = {
        "/", "/login", "/registro", "/recuperar", "/error",
        "/css/**", "/js/**", "/img/**", "/favicon.ico"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, @Value("${supabase.url}") String supabaseUrl) throws Exception {
        AuthenticationEntryPoint irALogin = SecurityConfig::redirigirALogin;

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(RUTAS_PUBLICAS).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .bearerTokenResolver(new CookieTokenResolver())
                .authenticationEntryPoint(irALogin)
                .jwt(jwt -> {}))
            .exceptionHandling(e -> e.authenticationEntryPoint(irALogin))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Token CSRF en cookie (no en sesión): así un entrenamiento largo
            // no falla al guardar porque haya caducado la sesión del servidor.
            .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository()))
            .headers(h -> h
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
                .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

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

    /** Lee el token de la cookie, salvo en rutas públicas (un token caducado no debe bloquearlas). */
    private static final class CookieTokenResolver implements BearerTokenResolver {
        @Override
        public String resolve(HttpServletRequest request) {
            String uri = request.getRequestURI();
            for (String publica : RUTAS_PUBLICAS) {
                boolean coincide = publica.endsWith("/**")
                    ? uri.startsWith(publica.substring(0, publica.length() - 2))
                    : uri.equals(publica);
                if (coincide) {
                    return null;
                }
            }
            Cookie cookie = WebUtils.getCookie(request, SESSION_COOKIE);
            return cookie != null && !cookie.getValue().isBlank() ? cookie.getValue() : null;
        }
    }
}

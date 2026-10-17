package com.gymtracker.gymtracker.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee el token de Supabase de la cookie "gt_session", lo verifica y, si es
 * válido, deja al usuario autenticado para el resto de la petición.
 *
 * Si falta, está caducado o es falso, la petición sigue como anónima: las
 * páginas públicas funcionan igual y las privadas redirigen al login.
 *
 * (Usamos un filtro propio en lugar de oauth2ResourceServer() porque este
 * último desactiva la protección CSRF cuando hay token, pensando en tokens
 * enviados en una cabecera. Nuestro token va en una cookie, que el navegador
 * envía solo, así que necesitamos CSRF.)
 */
public class SupabaseCookieFilter extends OncePerRequestFilter {

    public static final String COOKIE = "gt_session";

    private final JwtDecoder jwtDecoder;
    private final SecurityContextHolderStrategy contextos = SecurityContextHolder.getContextHolderStrategy();

    public SupabaseCookieFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Cookie cookie = WebUtils.getCookie(request, COOKIE);
        boolean yaAutenticado = contextos.getContext().getAuthentication() != null;

        if (cookie != null && !cookie.getValue().isBlank() && !yaAutenticado) {
            try {
                Jwt jwt = jwtDecoder.decode(cookie.getValue());
                SecurityContext contexto = contextos.createEmptyContext();
                contexto.setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
                contextos.setContext(contexto);
            } catch (JwtException e) {
                // Token caducado o no válido: seguimos sin usuario.
                logger.debug("Token de sesión no válido: " + e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    /** Los archivos estáticos no necesitan usuario: nos ahorramos verificar el token. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/img/") || uri.equals("/favicon.ico");
    }
}
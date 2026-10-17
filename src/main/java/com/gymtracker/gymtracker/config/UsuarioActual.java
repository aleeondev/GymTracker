package com.gymtracker.gymtracker.config;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Acceso cómodo al usuario autenticado (datos sacados del JWT de Supabase). */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    /** Id del usuario en Supabase (auth.users.id). */
    public static UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    public static boolean autenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken;
    }

    /** Nombre guardado al registrarse (user_metadata.nombre) o, si no hay, la parte inicial del email. */
    public static String nombre() {
        Jwt jwt = jwt();
        Object metadata = jwt.getClaims().get("user_metadata");
        if (metadata instanceof Map<?, ?> datos) {
            for (String clave : new String[] { "nombre", "name", "full_name" }) {
                Object valor = datos.get(clave);
                if (valor instanceof String texto && !texto.isBlank()) {
                    return texto.trim();
                }
            }
        }
        String email = jwt.getClaimAsString("email");
        return email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "deportista";
    }

    private static Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new IllegalStateException("No hay ningún usuario autenticado");
    }
}

package com.gymtracker.gymtracker.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Operaciones de ADMINISTRADOR sobre Supabase Auth (la tabla auth.users).
 * Necesitan la clave secreta del proyecto (supabase.secret-key), que solo
 * existe en el servidor: en local, en application-local.properties; en Render,
 * como variable de entorno SUPABASE_SECRET_KEY. Nunca en el navegador ni en git.
 */
@Component
public class SupabaseAdmin {

    private final String supabaseUrl;
    private final String claveSecreta;
    private final RestClient http = RestClient.create();

    public SupabaseAdmin(@Value("${supabase.url}") String supabaseUrl,
                         @Value("${supabase.secret-key:}") String claveSecreta) {
        this.supabaseUrl = supabaseUrl;
        this.claveSecreta = claveSecreta;
    }

    public boolean configurado() {
        return claveSecreta != null && !claveSecreta.isBlank();
    }

    /**
     * Borra la cuenta de acceso (email y contraseña) y cierra todas sus sesiones.
     * Si ya no existía, se da por bien hecho. Cualquier otro fallo lanza una excepción.
     */
    public void eliminarUsuario(UUID usuarioId) {
        if (!configurado()) {
            throw new IllegalStateException("Falta supabase.secret-key: no se pueden borrar cuentas.");
        }
        try {
            http.delete()
                .uri(supabaseUrl + "/auth/v1/admin/users/{id}", usuarioId)
                // Claves nuevas (sb_secret_...): basta "apikey" y Supabase se encarga del resto.
                // Clave antigua (service_role, un JWT): también va en Authorization.
                .headers(h -> {
                    h.set("apikey", claveSecreta);
                    if (!claveSecreta.startsWith("sb_")) {
                        h.setBearerAuth(claveSecreta);
                    }
                })
                .retrieve()
                .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // Ya no existía: lo que queríamos
        }
    }
}

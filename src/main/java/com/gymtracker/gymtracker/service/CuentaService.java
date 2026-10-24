package com.gymtracker.gymtracker.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Eliminar mi cuenta" (Mi perfil): borra TODOS los datos de una persona y su
 * cuenta de acceso de Supabase. Es definitivo.
 *
 * Todo va en una transacción: primero nuestras tablas y, al final, Supabase.
 * Si Supabase falla, se lanza una excepción y la transacción deshace los
 * borrados, así que nunca queda una cuenta a medias.
 *
 * Si se crea una tabla nueva con datos de usuario, hay que añadirla aquí
 * (y en la página de privacidad).
 */
@Service
public class CuentaService {

    private static final Logger log = LoggerFactory.getLogger(CuentaService.class);

    /** Lo que hay que escribir para confirmar (sin importar mayúsculas). */
    public static final String PALABRA_CONFIRMACION = "ELIMINAR";

    /**
     * Orden de borrado: primero lo que apunta a otras tablas (las series apuntan a
     * entrenamientos y a ejercicios; los entrenamientos, a rutinas...).
     */
    private static final String[] BORRADOS = {
        "delete from series_entrenamiento where entrenamiento_id in (select id from entrenamientos where usuario_id = ?)",
        "delete from entrenamientos where usuario_id = ?",
        "delete from rutina_ejercicios where rutina_id in (select id from rutinas where usuario_id = ?)",
        "delete from rutinas where usuario_id = ?",
        "delete from ejercicios where usuario_id = ?",
        "delete from pesos_corporales where usuario_id = ?",
        "delete from opiniones where usuario_id = ?",
        "delete from perfiles where usuario_id = ?"
    };

    private final JdbcTemplate jdbc;
    private final SupabaseAdmin supabaseAdmin;

    public CuentaService(JdbcTemplate jdbc, SupabaseAdmin supabaseAdmin) {
        this.jdbc = jdbc;
        this.supabaseAdmin = supabaseAdmin;
    }

    /** Devuelve un mensaje de error o null si la cuenta quedó eliminada. */
    @Transactional
    public String eliminar(UUID usuarioId, String confirmacion) {
        if (confirmacion == null || !PALABRA_CONFIRMACION.equalsIgnoreCase(confirmacion.strip())) {
            return "Para confirmar, escribe " + PALABRA_CONFIRMACION + " en el campo.";
        }
        if (!supabaseAdmin.configurado()) {
            return "Ahora mismo no se pueden eliminar cuentas. Escríbenos y la borramos nosotros.";
        }

        for (String sql : BORRADOS) {
            jdbc.update(sql, usuarioId);
        }
        // Al final: si esto falla, la excepción deshace todos los borrados de arriba
        supabaseAdmin.eliminarUsuario(usuarioId);
        log.info("Cuenta eliminada: {}", usuarioId);
        return null;
    }
}

package com.gymtracker.gymtracker.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.service.CuentaService;

/**
 * "Eliminar mi cuenta" (Mi perfil · js/perfil.js). Responde JSON: el navegador,
 * si sale bien, cierra también la sesión de Supabase guardada en el navegador.
 * Necesita sesión y token CSRF, como cualquier POST de la app.
 */
@RestController
public class CuentaController {

    private static final Logger log = LoggerFactory.getLogger(CuentaController.class);

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping("/perfil/eliminar-cuenta")
    public ResponseEntity<Map<String, String>> eliminar(@RequestParam(required = false) String confirmacion) {
        String error;
        try {
            error = cuentaService.eliminar(UsuarioActual.id(), confirmacion);
        } catch (RuntimeException e) {
            // Supabase no respondió o rechazó la clave: no se ha borrado nada (rollback)
            log.error("No se pudo eliminar la cuenta {}", UsuarioActual.id(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "No se ha podido eliminar la cuenta. No se ha borrado nada: inténtalo más tarde."));
        }
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        // Borramos ya la cookie de sesión del servidor
        ResponseCookie sinSesion = ResponseCookie.from("gt_session", "").path("/").maxAge(0).sameSite("Lax").build();
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, sinSesion.toString())
            .body(Map.of("ok", "Tu cuenta y todos tus datos se han eliminado."));
    }
}

package com.gymtracker.gymtracker.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.service.OpinionService;

/**
 * Botón "Opinar" (fragments/app.html + js/opinion.js). Se envía con fetch y
 * responde JSON, así no se recarga la página: si estás entrenando, no pierdes nada.
 * Necesita sesión y token CSRF, como cualquier otro POST de la app.
 */
@RestController
public class OpinionController {

    private final OpinionService opinionService;

    public OpinionController(OpinionService opinionService) {
        this.opinionService = opinionService;
    }

    @PostMapping("/opinion")
    public ResponseEntity<Map<String, String>> enviar(@RequestParam(required = false) String tipo,
                                                      @RequestParam(required = false) String texto,
                                                      @RequestParam(required = false) String pagina,
                                                      @RequestHeader(value = "User-Agent", required = false) String navegador) {
        // La página la manda el navegador: solo guardamos rutas nuestras
        String paginaValida = EjercicioController.rutaInternaValida(pagina) ? pagina : null;
        String error = opinionService.enviar(UsuarioActual.id(), tipo, texto, paginaValida, navegador);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        return ResponseEntity.ok(Map.of("ok", "¡Gracias! Lo leemos todo."));
    }
}

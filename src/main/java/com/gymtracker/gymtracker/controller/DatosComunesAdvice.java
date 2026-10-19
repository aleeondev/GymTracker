package com.gymtracker.gymtracker.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.dto.OpcionesPerfil;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

/** Datos comunes a todas las páginas (cabecera de la app, CSRF...). */
@ControllerAdvice
public class DatosComunesAdvice {

    /** Rutas que pertenecen al área "Entrenamiento" (muestran su barra secundaria). */
    private static final List<String> RUTAS_ENTRENO = List.of(
        "/entrenamiento", "/rutinas", "/ejercicios", "/historial", "/progreso", "/records");

    @ModelAttribute
    public void datosComunes(Model model, HttpServletRequest request) {
        if (UsuarioActual.autenticado()) {
            model.addAttribute("usuarioNombre", UsuarioActual.nombre());
        }
        String ruta = request.getRequestURI();
        model.addAttribute("rutaActual", ruta);
        model.addAttribute("area", area(ruta));
        // Textos de las opciones del cuestionario (objetivo, experiencia...) para las vistas
        model.addAttribute("opcionesPerfil", OpcionesPerfil.TODAS);

        // Genera ya el token CSRF (y su cookie) antes de empezar a pintar la
        // página; si se generara a mitad, la respuesta podría estar ya enviada.
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            csrf.getToken();
        }
    }

    /** En qué área de la app estás: panel, entreno, nutricion, coach (o null: perfil...). */
    static String area(String ruta) {
        if (ruta.equals("/dashboard")) {
            return "panel";
        }
        if (ruta.startsWith("/nutricion")) {
            return "nutricion";
        }
        if (ruta.startsWith("/coach")) {
            return "coach";
        }
        return RUTAS_ENTRENO.stream().anyMatch(ruta::startsWith) ? "entreno" : null;
    }
}

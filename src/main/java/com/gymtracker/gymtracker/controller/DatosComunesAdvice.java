package com.gymtracker.gymtracker.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import com.gymtracker.gymtracker.config.UsuarioActual;

import jakarta.servlet.http.HttpServletRequest;

/** Datos comunes a todas las páginas (cabecera de la app, CSRF...). */
@ControllerAdvice
public class DatosComunesAdvice {

    @ModelAttribute
    public void datosComunes(Model model, HttpServletRequest request) {
        if (UsuarioActual.autenticado()) {
            model.addAttribute("usuarioNombre", UsuarioActual.nombre());
        }
        model.addAttribute("rutaActual", request.getRequestURI());

        // Genera ya el token CSRF (y su cookie) antes de empezar a pintar la
        // página; si se generara a mitad, la respuesta podría estar ya enviada.
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            csrf.getToken();
        }
    }
}
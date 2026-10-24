package com.gymtracker.gymtracker.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Páginas legales (públicas). El responsable y el email de contacto salen de
 * application.properties (app.responsable.nombre, app.contacto.email) para
 * poder cambiarlos sin tocar la plantilla.
 */
@Controller
public class LegalController {

    private final String responsable;
    private final String contacto;

    public LegalController(@Value("${app.responsable.nombre}") String responsable,
                           @Value("${app.contacto.email}") String contacto) {
        this.responsable = responsable;
        this.contacto = contacto;
    }

    @GetMapping("/privacidad")
    public String privacidad(Model model) {
        model.addAttribute("responsable", responsable);
        model.addAttribute("contacto", contacto);
        return "privacidad";
    }
}

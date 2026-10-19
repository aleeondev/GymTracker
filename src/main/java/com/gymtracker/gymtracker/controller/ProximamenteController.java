package com.gymtracker.gymtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Páginas de funciones que llegarán en futuras versiones (Nutrición y Coach IA).
 * Muestran una vista previa desenfocada con datos de ejemplo y un cartel "Próximamente".
 * Cuando se construyan de verdad, cada una tendrá su propio controlador.
 */
@Controller
public class ProximamenteController {

    @GetMapping("/nutricion")
    public String nutricion() {
        return "nutricion";
    }

    @GetMapping("/coach")
    public String coach() {
        return "coach";
    }
}

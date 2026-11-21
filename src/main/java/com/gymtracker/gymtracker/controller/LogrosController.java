package com.gymtracker.gymtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.service.LogrosService;

/** Página "Mis logros": las medallas del usuario. */
@Controller
public class LogrosController {

    private final LogrosService logrosService;

    public LogrosController(LogrosService logrosService) {
        this.logrosService = logrosService;
    }

    @GetMapping("/logros")
    public String logros(Model model) {
        model.addAttribute("resumen", logrosService.logros(UsuarioActual.id()));
        return "logros";
    }
}

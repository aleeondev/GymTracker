package com.gymtracker.gymtracker.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.service.EntrenamientoService;
import com.gymtracker.gymtracker.service.RutinaService;

@Controller
public class DashboardController {

    private final EntrenamientoService entrenamientoService;
    private final RutinaService rutinaService;

    public DashboardController(EntrenamientoService entrenamientoService, RutinaService rutinaService) {
        this.entrenamientoService = entrenamientoService;
        this.rutinaService = rutinaService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        UUID usuarioId = UsuarioActual.id();
        model.addAttribute("resumen", entrenamientoService.resumen(usuarioId));
        model.addAttribute("rutinas", rutinaService.obtenerDelUsuario(usuarioId));
        model.addAttribute("hoy", EntrenamientoService.hoy());
        return "dashboard";
    }
}
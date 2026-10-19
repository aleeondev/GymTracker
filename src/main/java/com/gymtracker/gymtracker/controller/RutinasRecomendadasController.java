package com.gymtracker.gymtracker.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.model.Perfil;
import com.gymtracker.gymtracker.service.PerfilService;
import com.gymtracker.gymtracker.service.RutinasRecomendadasService;

/** Rutinas recomendadas: planes ya montados que se crean en tu cuenta con un toque. */
@Controller
public class RutinasRecomendadasController {

    private final RutinasRecomendadasService recomendadasService;
    private final PerfilService perfilService;

    public RutinasRecomendadasController(RutinasRecomendadasService recomendadasService, PerfilService perfilService) {
        this.recomendadasService = recomendadasService;
        this.perfilService = perfilService;
    }

    @GetMapping("/rutinas/recomendadas")
    public String ver(Model model) {
        Perfil perfil = perfilService.obtener(UsuarioActual.id());
        model.addAttribute("perfil", perfil);
        model.addAttribute("planes", RutinasRecomendadasService.PLANES);
        model.addAttribute("recomendado", recomendadasService.recomendado(perfil));
        model.addAttribute("esquema", recomendadasService.esquema(perfil));
        return "rutinas-recomendadas";
    }

    @PostMapping("/rutinas/recomendadas/crear")
    public String crear(@RequestParam String plan, RedirectAttributes redirect) {
        UUID usuarioId = UsuarioActual.id();
        try {
            List<String> creadas = recomendadasService.crear(plan, perfilService.obtener(usuarioId), usuarioId);
            redirect.addFlashAttribute("exito", "¡Listo! Rutinas creadas: " + String.join(", ", creadas) + ".");
            return "redirect:/rutinas";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", "Ese plan no existe.");
            return "redirect:/rutinas/recomendadas";
        }
    }
}

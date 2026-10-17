package com.gymtracker.gymtracker.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.dto.PerfilForm;
import com.gymtracker.gymtracker.model.Perfil;
import com.gymtracker.gymtracker.service.PerfilService;

/**
 * Cuestionario de bienvenida (3 pasos: sobre ti, tu objetivo, tu rutina).
 *  - La primera vez que entras al panel te trae aquí (ver DashboardController).
 *  - Desde "Mi perfil" se vuelve aquí con ?editar para cambiar los datos.
 */
@Controller
public class BienvenidaController {

    private final PerfilService perfilService;

    public BienvenidaController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/bienvenida")
    public String bienvenida(@RequestParam(required = false) String editar, Model model) {
        Perfil perfil = perfilService.obtener(UsuarioActual.id());
        return pagina(PerfilForm.desde(perfil), perfil, editar != null, model);
    }

    @PostMapping("/bienvenida/guardar")
    public String guardar(@ModelAttribute PerfilForm form, @RequestParam(required = false) String editar,
                          Model model, RedirectAttributes redirect) {
        UUID usuarioId = UsuarioActual.id();
        String error = perfilService.guardarCuestionario(usuarioId, form);
        if (error != null) {
            // Volvemos a pintar el formulario con lo que había escrito (sin perderlo)
            model.addAttribute("error", error);
            return pagina(form, perfilService.obtener(usuarioId), editar != null, model);
        }
        if (editar != null) {
            redirect.addFlashAttribute("exito", "Datos actualizados.");
            return "redirect:/perfil";
        }
        redirect.addFlashAttribute("exito", "¡Perfil listo! Ya puedes empezar a entrenar.");
        return "redirect:/dashboard";
    }

    @PostMapping("/bienvenida/saltar")
    public String saltar() {
        perfilService.saltarBienvenida(UsuarioActual.id());
        return "redirect:/dashboard";
    }

    private String pagina(PerfilForm form, Perfil perfil, boolean editando, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("editando", editando);
        model.addAttribute("pideConsentimiento", perfil.getConsentimientoEn() == null);
        return "bienvenida";
    }
}

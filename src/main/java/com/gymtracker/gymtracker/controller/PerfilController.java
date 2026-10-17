package com.gymtracker.gymtracker.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.service.EntrenamientoService;
import com.gymtracker.gymtracker.service.PerfilService;

/**
 * Página "Mi perfil".
 *
 * Qué se guarda dónde:
 *  - Nombre visible y contraseña: en Supabase Auth. Se cambian desde el navegador
 *    con la librería de Supabase (js/perfil.js), no pasan por este controlador.
 *  - Nombre de usuario (y en la idea 6, los datos físicos y el objetivo): en
 *    nuestra tabla "perfiles", con los formularios POST de aquí.
 */
@Controller
public class PerfilController {

    private final PerfilService perfilService;
    private final EntrenamientoService entrenamientoService;

    public PerfilController(PerfilService perfilService, EntrenamientoService entrenamientoService) {
        this.perfilService = perfilService;
        this.entrenamientoService = entrenamientoService;
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {
        UUID usuarioId = UsuarioActual.id();
        model.addAttribute("perfil", perfilService.obtener(usuarioId));
        model.addAttribute("email", UsuarioActual.email());
        model.addAttribute("totalEntrenos", entrenamientoService.resumen(usuarioId).totalEntrenos());
        return "perfil";
    }

    /**
     * Para el registro: ¿está libre este nombre de usuario? Responde en JSON,
     * por ejemplo {"estado": "libre"}. Es pública (al registrarte aún no tienes
     * sesión). Los nombres de usuario son públicos, así que no revela nada privado.
     */
    @GetMapping("/api/usuario-disponible")
    @ResponseBody
    public Map<String, String> usuarioDisponible(@RequestParam(defaultValue = "") String u) {
        return Map.of("estado", perfilService.disponibilidad(u));
    }

    @PostMapping("/perfil/usuario")
    public String guardarUsuario(@RequestParam(defaultValue = "") String nombreUsuario, RedirectAttributes redirect) {
        String error = perfilService.guardarNombreUsuario(UsuarioActual.id(), nombreUsuario);
        if (error != null) {
            redirect.addFlashAttribute("error", error);
        } else {
            redirect.addFlashAttribute("exito", "Nombre de usuario guardado.");
        }
        return "redirect:/perfil";
    }
}

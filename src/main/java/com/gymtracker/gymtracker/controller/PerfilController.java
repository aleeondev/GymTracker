package com.gymtracker.gymtracker.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gymtracker.gymtracker.config.Formato;
import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.model.PesoCorporal;
import com.gymtracker.gymtracker.service.EntrenamientoService;
import com.gymtracker.gymtracker.service.PerfilService;
import com.gymtracker.gymtracker.service.PesoCorporalService;

import tools.jackson.databind.json.JsonMapper;

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

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final PerfilService perfilService;
    private final EntrenamientoService entrenamientoService;
    private final PesoCorporalService pesoCorporalService;
    private final Formato fmt;

    public PerfilController(PerfilService perfilService, EntrenamientoService entrenamientoService,
                            PesoCorporalService pesoCorporalService, Formato fmt) {
        this.perfilService = perfilService;
        this.entrenamientoService = entrenamientoService;
        this.pesoCorporalService = pesoCorporalService;
        this.fmt = fmt;
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {
        UUID usuarioId = UsuarioActual.id();
        model.addAttribute("perfil", perfilService.obtener(usuarioId));
        model.addAttribute("email", UsuarioActual.email());
        model.addAttribute("totalEntrenos", entrenamientoService.resumen(usuarioId).totalEntrenos());

        // Gráfica "Tu peso" (js/grafica-peso.js): un punto por registro
        List<PesoCorporal> pesos = pesoCorporalService.historial(usuarioId);
        List<Map<String, Object>> puntos = new ArrayList<>();
        for (PesoCorporal p : pesos) {
            puntos.add(Map.of("fecha", p.getFecha().toString(), "etiqueta", fmt.fecha(p.getFecha()), "peso", p.getPesoKg()));
        }
        model.addAttribute("numPesos", pesos.size());
        model.addAttribute("pesosJson", JSON.writeValueAsString(puntos));
        model.addAttribute("hoy", EntrenamientoService.hoy());
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

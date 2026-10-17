package com.gymtracker.gymtracker.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.model.Ejercicio;
import com.gymtracker.gymtracker.service.EjercicioService;

@Controller 
@RequestMapping("/ejercicios")
public class EjercicioController {
    private final EjercicioService ejercicioService;

    public EjercicioController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }

    /** Solo se aceptan estos campos del formulario (evita que se "cuelen" otros, como usuarioId). */
    @InitBinder("ejercicio")
    void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("id", "nombre", "grupoMuscular", "descripcion", "equipamiento");
    }

    @GetMapping 
    public String listar(@RequestParam(required = false) String grupo, Model model) {
        List<Ejercicio> ejercicios = ejercicioService.obtenerVisibles(UsuarioActual.id());
        if (grupo != null && !grupo.isBlank()) {
            ejercicios = ejercicios.stream().filter(e -> grupo.equals(e.getGrupoMuscular())).toList();
        }
        model.addAttribute("propios", ejercicios.stream().filter(e -> !e.esComun()).toList());
        model.addAttribute("comunes", ejercicios.stream().filter(Ejercicio::esComun).toList());
        model.addAttribute("grupos", EjercicioService.GRUPOS_MUSCULARES);
        model.addAttribute("grupoSeleccionado", grupo);
        return "ejercicios";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) String volver, Model model) {
        model.addAttribute("ejercicio", new Ejercicio());
        return formulario(model, volver);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("ejercicio", ejercicioService.obtenerPropio(id, UsuarioActual.id()));
        return formulario(model, null);
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Ejercicio ejercicio, @RequestParam(required = false) String volver,
                          RedirectAttributes redirect) {
        if (ejercicio.getNombre() == null || ejercicio.getNombre().isBlank()) {
            redirect.addFlashAttribute("error", "El ejercicio necesita un nombre.");
            return "redirect:/ejercicios/nuevo";
        }
        Ejercicio guardado = ejercicioService.guardar(ejercicio, UsuarioActual.id());
        redirect.addFlashAttribute("exito", "Ejercicio \"" + guardado.getNombre() + "\" guardado.");
        return "redirect:" + (rutaInternaValida(volver) ? volver : "/ejercicios");
    }

    @PostMapping("/eliminar/{id}") 
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        String error = ejercicioService.eliminar(id, UsuarioActual.id());
        if (error != null) {
            redirect.addFlashAttribute("error", error);
        } else {
            redirect.addFlashAttribute("exito", "Ejercicio eliminado.");
        }
        return "redirect:/ejercicios";
    }

    private String formulario(Model model, String volver) {
        model.addAttribute("grupos", EjercicioService.GRUPOS_MUSCULARES);
        model.addAttribute("volver", rutaInternaValida(volver) ? volver : null);
        return "formulario-ejercicio";
    }

    /** Solo se permite volver a rutas de nuestra propia web (evita redirecciones a otras webs). */
    static boolean rutaInternaValida(String ruta) {
        return ruta != null && ruta.startsWith("/") && !ruta.startsWith("//") && !ruta.contains("\\");
    }
}
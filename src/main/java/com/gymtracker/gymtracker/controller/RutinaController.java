package com.gymtracker.gymtracker.controller;

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
import com.gymtracker.gymtracker.model.Rutina;
import com.gymtracker.gymtracker.model.RutinaEjercicio;
import com.gymtracker.gymtracker.service.EjercicioService;
import com.gymtracker.gymtracker.service.RutinaService;

@Controller
@RequestMapping("/rutinas")
public class RutinaController {
    private final RutinaService rutinaService;
    private final EjercicioService ejercicioService;

    public RutinaController(RutinaService rutinaService, EjercicioService ejercicioService) {
        this.rutinaService = rutinaService;
        this.ejercicioService = ejercicioService;
    }

    @InitBinder("rutina")
    void camposRutina(WebDataBinder binder) {
        binder.setAllowedFields("id", "nombre", "descripcion");
    }

    @InitBinder("rutinaEjercicio")
    void camposRutinaEjercicio(WebDataBinder binder) {
        binder.setAllowedFields("id", "series", "repeticiones", "pesoObjetivo", "descanso");
    }

    @GetMapping 
    public String listar(Model model) {
        model.addAttribute("rutinas", rutinaService.obtenerDelUsuario(UsuarioActual.id()));
        return "rutinas";
    }

    @GetMapping("/nuevo") 
    public String nuevo(Model model) {
        model.addAttribute("rutina", new Rutina());
        return "formulario-rutina";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Rutina rutina, RedirectAttributes redirect) {
        if (rutina.getNombre() == null || rutina.getNombre().isBlank()) {
            redirect.addFlashAttribute("error", "La rutina necesita un nombre.");
            return "redirect:/rutinas/nuevo";
        }
        boolean nueva = rutina.getId() == null;
        Rutina guardada = rutinaService.guardar(rutina, UsuarioActual.id());
        redirect.addFlashAttribute("exito", nueva ? "Rutina creada. Ahora añade sus ejercicios." : "Rutina guardada.");
        return "redirect:/rutinas/" + guardada.getId();
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("rutina", rutinaService.obtener(id, UsuarioActual.id()));
        return "formulario-rutina";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        rutinaService.eliminar(id, UsuarioActual.id());
        redirect.addFlashAttribute("exito", "Rutina eliminada. Tu historial se conserva.");
        return "redirect:/rutinas";
    }

    @GetMapping("/{id}")
    public String verRutina(@PathVariable Long id, Model model) {
        model.addAttribute("rutina", rutinaService.obtener(id, UsuarioActual.id()));
        return "detalle-rutina";
    }

    // ---------- Ejercicios dentro de la rutina ----------

    @GetMapping("/{id}/ejercicios/nuevo")
    public String nuevoEjercicio(@PathVariable Long id, Model model) {
        RutinaEjercicio nuevo = new RutinaEjercicio();
        nuevo.setSeries(3);
        nuevo.setRepeticiones(10);
        nuevo.setDescanso(90);
        return formularioEjercicio(id, nuevo, null, model);
    }

    @GetMapping("/{id}/ejercicios/{reId}/editar")
    public String editarEjercicio(@PathVariable Long id, @PathVariable Long reId, Model model) {
        RutinaEjercicio re = rutinaService.obtenerEjercicio(id, reId, UsuarioActual.id());
        return formularioEjercicio(id, re, re.getEjercicio().getId(), model);
    }

    // Ojo: la variable de la URL se llama {rutinaId} y no {id} porque Spring también
    // copia las variables de la URL al objeto del formulario, y pisaría rutinaEjercicio.id.
    @PostMapping("/{rutinaId}/ejercicios/guardar")
    public String guardarEjercicio(@PathVariable Long rutinaId, @ModelAttribute RutinaEjercicio rutinaEjercicio,
                                   @RequestParam Long ejercicioId, RedirectAttributes redirect) {
        rutinaService.guardarEjercicio(rutinaId, rutinaEjercicio, ejercicioId, UsuarioActual.id());
        redirect.addFlashAttribute("exito", "Ejercicio guardado en la rutina.");
        return "redirect:/rutinas/" + rutinaId;
    }

    @PostMapping("/{id}/ejercicios/{reId}/eliminar")
    public String eliminarEjercicio(@PathVariable Long id, @PathVariable Long reId) {
        rutinaService.eliminarEjercicio(id, reId, UsuarioActual.id());
        return "redirect:/rutinas/" + id;
    }

    @PostMapping("/{id}/ejercicios/{reId}/mover")
    public String moverEjercicio(@PathVariable Long id, @PathVariable Long reId, @RequestParam int direccion) {
        rutinaService.moverEjercicio(id, reId, direccion < 0 ? -1 : 1, UsuarioActual.id());
        return "redirect:/rutinas/" + id;
    }

    private String formularioEjercicio(Long rutinaId, RutinaEjercicio re, Long ejercicioSeleccionado, Model model) {
        UUID usuarioId = UsuarioActual.id();
        model.addAttribute("rutina", rutinaService.obtener(rutinaId, usuarioId));
        model.addAttribute("rutinaEjercicio", re);
        model.addAttribute("ejercicioSeleccionado", ejercicioSeleccionado);
        model.addAttribute("ejercicios", ejercicioService.obtenerVisibles(usuarioId));
        model.addAttribute("grupos", EjercicioService.GRUPOS_MUSCULARES);
        return "formulario-rutina-ejercicio";
    }
}
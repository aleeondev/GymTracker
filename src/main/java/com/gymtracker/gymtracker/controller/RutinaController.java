package com.gymtracker.gymtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gymtracker.gymtracker.model.Rutina;
import com.gymtracker.gymtracker.model.RutinaEjercicio;
import com.gymtracker.gymtracker.service.EjercicioService;
import com.gymtracker.gymtracker.service.RutinaEjercicioService;
import com.gymtracker.gymtracker.service.RutinaService;

@Controller
@RequestMapping("/rutinas")
public class RutinaController {
    private final RutinaService rutinaService;
    private final EjercicioService ejercicioService;
    private final RutinaEjercicioService rutinaEjercicioService;

    public RutinaController(RutinaService rutinaService, EjercicioService ejercicioService, RutinaEjercicioService rutinaEjercicioService) {
        this.rutinaService = rutinaService;
        this.ejercicioService = ejercicioService;
        this.rutinaEjercicioService = rutinaEjercicioService;
    }

    @GetMapping 
    public String listar(Model model) {
        model.addAttribute("rutinas", rutinaService.obtenerTodas());
        return "rutinas";
    }

    @GetMapping("/nuevo") 
    public String nuevo(Model model) {
        model.addAttribute("rutina", new Rutina());
        return "formulario-rutina";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Rutina rutina) {
        rutinaService.guardar(rutina);
        return "redirect:/rutinas";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Rutina rutina = rutinaService.obtenerPorId(id).orElseThrow();
        model.addAttribute("rutina", rutina);
        return "formulario-rutina";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        rutinaService.eliminar(id);
        return "redirect:/rutinas";
    }

    @GetMapping("/{id}")
    public String verRutina(@PathVariable Long id, Model model) {
        Rutina rutina = rutinaService.obtenerPorId(id).orElseThrow();

        model.addAttribute("rutina", rutina);
        model.addAttribute("rutinaEjercicios", rutinaEjercicioService.obtenerPorRutina(rutina));
        return "detalle-rutina";
    }

    // FORMULARIO PARA AÑADIR EJERCICIO
    @GetMapping("/{id}/ejercicios/nuevo")
    public String nuevoEjercicio(@PathVariable Long id, Model model) {
        Rutina rutina = rutinaService.obtenerPorId(id).orElseThrow();
        model.addAttribute("rutina", rutina);
        model.addAttribute("rutinaEjercicio", new RutinaEjercicio());
        model.addAttribute("ejercicios", ejercicioService.obtenerTodos());

        return "formulario-rutina-ejercicio";
    }

    // GUARDAR EJERCICIO DENTRO DE UNA RUTINA
    @PostMapping("/{id}/ejercicios/guardar")
    public String guardarEjercicio(@PathVariable Long id, @ModelAttribute RutinaEjercicio rutinaEjercicio) {
        Rutina rutina = rutinaService.obtenerPorId(id).orElseThrow();
        rutinaEjercicio.setId(null);
        rutinaEjercicio.setRutina(rutina);
        int siguienteOrden = rutinaEjercicioService.obtenerPorRutina(rutina).size() + 1;
        rutinaEjercicio.setOrden(siguienteOrden);
        rutinaEjercicioService.guardar(rutinaEjercicio);
        return "redirect:/rutinas/" + id;
    }

}
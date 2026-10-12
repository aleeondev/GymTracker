package com.gymtracker.gymtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gymtracker.gymtracker.model.Ejercicio;
import com.gymtracker.gymtracker.service.EjercicioService;

@Controller 
@RequestMapping("/ejercicios")
public class EjercicioController {
    private final EjercicioService ejercicioService;

    public EjercicioController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }   

    @GetMapping 
    public String listar(Model model) {
        model.addAttribute("ejercicios", ejercicioService.obtenerTodos());
        return "ejercicios";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model){ 
        model.addAttribute("ejercicio", new Ejercicio());
        return "formulario-ejercicio";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Ejercicio ejercicio) {
        ejercicioService.guardar(ejercicio);
        return "redirect:/ejercicios";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Ejercicio ejercicio = ejercicioService.obtenerPorId(id).orElseThrow();
        model.addAttribute("ejercicio", ejercicio);
        return "formulario-ejercicio";
    }

    @GetMapping("/eliminar/{id}") 
    public String eliminar(@PathVariable Long id) {
        ejercicioService.eliminar(id);
        return "redirect:/ejercicios";
    }
}

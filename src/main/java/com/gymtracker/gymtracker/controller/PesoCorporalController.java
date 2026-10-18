package com.gymtracker.gymtracker.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gymtracker.gymtracker.config.Formato;
import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.service.PesoCorporalService;

/** Registrar el peso corporal (desde el panel o desde Mi perfil). */
@Controller
public class PesoCorporalController {

    private final PesoCorporalService pesoCorporalService;
    private final Formato fmt;

    public PesoCorporalController(PesoCorporalService pesoCorporalService, Formato fmt) {
        this.pesoCorporalService = pesoCorporalService;
        this.fmt = fmt;
    }

    @PostMapping("/peso/registrar")
    public String registrar(@RequestParam(required = false) Double pesoKg,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                            @RequestParam(defaultValue = "/dashboard") String volver,
                            RedirectAttributes redirect) {
        String error = pesoCorporalService.registrar(UsuarioActual.id(), pesoKg, fecha);
        if (error != null) {
            redirect.addFlashAttribute("error", error);
        } else {
            redirect.addFlashAttribute("exito", "Peso registrado: " + fmt.kg(pesoKg) + ".");
        }
        // Solo volvemos a páginas nuestras conocidas (nunca a una dirección que venga de fuera)
        return "redirect:" + ("/perfil".equals(volver) ? "/perfil" : "/dashboard");
    }
}

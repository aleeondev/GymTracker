package com.gymtracker.gymtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Páginas públicas (no necesitan sesión). */
@Controller 
public class HomeController {
    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    @GetMapping("/login") 
    public String login() {
        return "login";
    }

    /** Paso 1 de "¿Has olvidado tu contraseña?": pedir el email. */
    @GetMapping("/recuperar")
    public String recuperar() {
        return "recuperar";
    }

    /** Paso 2: el enlace del email trae aquí para elegir la contraseña nueva. */
    @GetMapping("/nueva-contrasena")
    public String nuevaContrasena() {
        return "nueva-contrasena";
    }

    /** Enlace de un email de Supabase caducado o no válido (ver enlace-error-redirect.js). */
    @GetMapping("/enlace-error")
    public String enlaceError() {
        return "enlace-error";
    }
}
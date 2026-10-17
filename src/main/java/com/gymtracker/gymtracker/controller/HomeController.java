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
}
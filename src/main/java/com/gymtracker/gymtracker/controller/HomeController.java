package com.gymtracker.gymtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller 
public class HomeController {
    @GetMapping("/")
    public String inicio() {
        return "index";
    }
    
    @GetMapping("/entrenamiento") 
    public String entrenamiento(){
        return "entrenamiento";
    }

    @GetMapping("/historial")
    public String historial(){
        return "historial";
    }

    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    @GetMapping("/login") 
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
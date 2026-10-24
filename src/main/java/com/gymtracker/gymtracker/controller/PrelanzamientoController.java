package com.gymtracker.gymtracker.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gymtracker.gymtracker.config.LimitePeticiones;
import com.gymtracker.gymtracker.dto.InteresadoForm;
import com.gymtracker.gymtracker.service.InteresadoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Web de prelanzamiento (/pronto): pantalla de carga, Fitness Challenge y
 * lista de acceso anticipado. Es pública (ver RUTAS_PUBLICAS).
 *
 * El formulario se envía con fetch (pronto.js) y recibe JSON. Si el
 * navegador no tiene JavaScript, el mismo formulario funciona "a la antigua"
 * y vuelve a /pronto con ?apuntado o ?error.
 *
 * Privacidad: la respuesta es la misma tanto si el email es nuevo como si ya
 * estaba, para que nadie pueda averiguar quién está apuntado.
 */
@Controller
@RequestMapping("/pronto")
public class PrelanzamientoController {

    static final String MENSAJE_OK = "Estás dentro. Te avisaremos antes que a nadie.";
    static final String MENSAJE_LIMITE = "Demasiados intentos seguidos. Prueba de nuevo en unos minutos.";

    private final InteresadoService interesadoService;
    private final LimitePeticiones limite;

    public PrelanzamientoController(InteresadoService interesadoService, LimitePeticiones limite) {
        this.interesadoService = interesadoService;
        this.limite = limite;
    }

    @InitBinder("interesadoForm")
    void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("email", "nombre", "puntuacion", "consentimiento", "web");
    }

    @GetMapping
    public String pronto() {
        return "pronto";
    }

    /** Envío desde pronto.js (pide JSON). */
    @PostMapping(value = "/apuntarme", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> apuntarmeJson(@Valid @ModelAttribute InteresadoForm interesadoForm,
                                                             BindingResult errores, HttpServletRequest request) {
        if (!limite.permitir(request.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", MENSAJE_LIMITE));
        }
        if (errores.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", primerError(errores)));
        }
        interesadoService.apuntar(interesadoForm);
        return ResponseEntity.ok(Map.of("mensaje", MENSAJE_OK));
    }

    /** Envío sin JavaScript (formulario HTML normal). */
    @PostMapping("/apuntarme")
    public String apuntarme(@Valid @ModelAttribute InteresadoForm interesadoForm,
                            BindingResult errores, HttpServletRequest request) {
        if (!limite.permitir(request.getRemoteAddr()) || errores.hasErrors()) {
            return "redirect:/pronto?error#apuntate";
        }
        interesadoService.apuntar(interesadoForm);
        return "redirect:/pronto?apuntado#apuntate";
    }

    /**
     * Enlace "darme de baja" de los correos. El GET solo pregunta: algunos
     * programas de correo abren los enlaces solos para revisarlos, y eso no
     * debe borrar a nadie. La baja de verdad es el POST del botón.
     */
    @GetMapping("/baja")
    public String confirmarBaja(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", interesadoService.buscarPorToken(token).isPresent() ? token : null);
        return "pronto-baja";
    }

    @PostMapping("/baja")
    public String darDeBaja(@RequestParam(required = false) String token, Model model) {
        interesadoService.darDeBaja(token);
        model.addAttribute("hecho", true);
        return "pronto-baja";
    }

    private static String primerError(BindingResult errores) {
        // Orden fijo y comprensible: primero el email, luego el permiso, luego lo demás
        for (String campo : new String[] { "email", "consentimiento", "nombre", "puntuacion" }) {
            if (errores.hasFieldErrors(campo)) {
                return errores.getFieldError(campo).getDefaultMessage();
            }
        }
        return "Revisa los datos del formulario.";
    }
}

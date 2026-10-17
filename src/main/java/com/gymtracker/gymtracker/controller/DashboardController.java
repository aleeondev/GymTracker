package com.gymtracker.gymtracker.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gymtracker.gymtracker.config.Formato;
import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.dto.SemanaVolumen;
import com.gymtracker.gymtracker.model.Perfil;
import com.gymtracker.gymtracker.service.EntrenamientoService;
import com.gymtracker.gymtracker.service.PerfilService;
import com.gymtracker.gymtracker.service.RutinaService;

import tools.jackson.databind.json.JsonMapper;

@Controller
public class DashboardController {

    /** Semanas que muestra la gráfica de volumen del panel. */
    private static final int SEMANAS_GRAFICA = 8;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final EntrenamientoService entrenamientoService;
    private final RutinaService rutinaService;
    private final PerfilService perfilService;
    private final Formato fmt;

    public DashboardController(EntrenamientoService entrenamientoService, RutinaService rutinaService,
                               PerfilService perfilService, Formato fmt) {
        this.entrenamientoService = entrenamientoService;
        this.rutinaService = rutinaService;
        this.perfilService = perfilService;
        this.fmt = fmt;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        UUID usuarioId = UsuarioActual.id();

        // Primera vez: creamos su perfil con lo que eligió al registrarse y, si aún
        // no ha visto el cuestionario de bienvenida, le llevamos a él.
        Perfil perfil = perfilService.asegurarPerfil(usuarioId,
            UsuarioActual.metadato("nombre_usuario"), UsuarioActual.metadato("consentimiento") != null);
        if (perfil.getBienvenidaEn() == null) {
            return "redirect:/bienvenida";
        }
        model.addAttribute("perfil", perfil);

        model.addAttribute("resumen", entrenamientoService.resumen(usuarioId));
        model.addAttribute("rutinas", rutinaService.obtenerDelUsuario(usuarioId));
        model.addAttribute("hoy", EntrenamientoService.hoy());
        prepararGraficaVolumen(usuarioId, model);
        return "dashboard";
    }

    /**
     * Datos de la gráfica "Volumen semanal". Van al HTML como JSON en data-datos
     * y los dibuja js/grafica-volumen.js con Chart.js.
     */
    private void prepararGraficaVolumen(UUID usuarioId, Model model) {
        List<SemanaVolumen> semanas = entrenamientoService.volumenSemanal(usuarioId, SEMANAS_GRAFICA);

        List<Map<String, Object>> barras = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < semanas.size(); i++) {
            SemanaVolumen s = semanas.get(i);
            boolean actual = i == semanas.size() - 1;
            Map<String, Object> barra = new LinkedHashMap<>();
            barra.put("etiqueta", fmt.diaMes(s.lunes()));                        // eje X: "13 oct"
            barra.put("titulo", (actual ? "Esta semana" : "Semana del " + fmt.diaMes(s.lunes())));
            barra.put("volumen", Math.round(s.volumen()));
            barra.put("entrenos", s.entrenos());
            barra.put("actual", actual);
            barras.add(barra);
            total += s.volumen();
        }
        double estaSemana = semanas.get(semanas.size() - 1).volumen();
        double media = total / semanas.size();

        model.addAttribute("hayVolumen", total > 0);
        model.addAttribute("volumenJson", JSON.writeValueAsString(barras));
        model.addAttribute("volumenEstaSemana", estaSemana);
        model.addAttribute("volumenMedia", media);
        // Resumen en texto para lectores de pantalla (la gráfica es una imagen para ellos)
        model.addAttribute("volumenDescripcion", "Volumen de las últimas " + SEMANAS_GRAFICA + " semanas. Esta semana: "
            + fmt.entero(estaSemana) + " kg. Media: " + fmt.entero(media) + " kg por semana.");
    }
}

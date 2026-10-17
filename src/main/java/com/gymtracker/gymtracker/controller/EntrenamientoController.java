package com.gymtracker.gymtracker.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gymtracker.gymtracker.config.Formato;
import com.gymtracker.gymtracker.config.UsuarioActual;
import com.gymtracker.gymtracker.dto.EntrenamientoForm;
import com.gymtracker.gymtracker.dto.ProgresoEjercicio;
import com.gymtracker.gymtracker.model.Ejercicio;
import com.gymtracker.gymtracker.model.Entrenamiento;
import com.gymtracker.gymtracker.model.Rutina;
import com.gymtracker.gymtracker.model.RutinaEjercicio;
import com.gymtracker.gymtracker.model.SerieEntrenamiento;
import com.gymtracker.gymtracker.service.EjercicioService;
import com.gymtracker.gymtracker.service.EntrenamientoService;
import com.gymtracker.gymtracker.service.RutinaService;

import tools.jackson.databind.json.JsonMapper;

/** Registrar entrenamientos, historial y progreso. */
@Controller
public class EntrenamientoController {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final EntrenamientoService entrenamientoService;
    private final RutinaService rutinaService;
    private final EjercicioService ejercicioService;
    private final Formato fmt;

    public EntrenamientoController(EntrenamientoService entrenamientoService, RutinaService rutinaService,
                                   EjercicioService ejercicioService, Formato fmt) {
        this.entrenamientoService = entrenamientoService;
        this.rutinaService = rutinaService;
        this.ejercicioService = ejercicioService;
        this.fmt = fmt;
    }

    // ---------- Entrenar ----------

    /** Elegir qué entrenar: una rutina o un entrenamiento libre. */
    @GetMapping("/entrenamiento")
    public String elegir(Model model) {
        model.addAttribute("rutinas", rutinaService.obtenerDelUsuario(UsuarioActual.id()));
        return "entrenamiento";
    }

    /** Formulario para registrar un entrenamiento nuevo (vacío o a partir de una rutina). */
    @GetMapping("/entrenamiento/nuevo")
    public String nuevo(@RequestParam(required = false) Long rutina, Model model) {
        UUID usuarioId = UsuarioActual.id();
        Map<Long, List<SerieEntrenamiento>> ultimaVez = entrenamientoService.ultimaVez(usuarioId, null);

        Map<String, Object> datos = new LinkedHashMap<>();
        List<Map<String, Object>> bloques = new ArrayList<>();
        String nombre = "Entrenamiento libre";
        if (rutina != null) {
            Rutina r = rutinaService.obtener(rutina, usuarioId);
            nombre = r.getNombre();
            datos.put("rutinaId", r.getId());
            for (RutinaEjercicio re : r.getEjercicios()) {
                Map<String, Object> bloque = bloque(re.getEjercicio(), re.getDescanso());
                bloque.put("objetivo", Map.of(
                    "series", re.getSeries() != null ? re.getSeries() : 3,
                    "reps", re.getRepeticiones() != null ? re.getRepeticiones() : 10,
                    "peso", re.getPesoObjetivo() != null ? re.getPesoObjetivo() : 0));
                bloque.put("series", List.of());
                bloques.add(bloque);
            }
        }
        datos.put("nombre", nombre);
        datos.put("fecha", EntrenamientoService.hoy().toString());
        datos.put("claveBorrador", "gt_borrador_" + usuarioId.toString().substring(0, 8) + "_" + (rutina != null ? "r" + rutina : "libre"));
        return formulario(datos, bloques, ultimaVez, usuarioId, model, false);
    }

    /** Editar un entrenamiento ya guardado. */
    @GetMapping("/historial/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        UUID usuarioId = UsuarioActual.id();
        Entrenamiento e = entrenamientoService.obtener(id, usuarioId);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", e.getId());
        if (e.getRutina() != null) {
            datos.put("rutinaId", e.getRutina().getId());
        }
        datos.put("nombre", e.getNombre());
        datos.put("fecha", e.getFecha().toString());
        datos.put("duracionMinutos", e.getDuracionMinutos());
        datos.put("notas", e.getNotas());
        datos.put("claveBorrador", "gt_borrador_" + usuarioId.toString().substring(0, 8) + "_e" + e.getId());

        List<Map<String, Object>> bloques = new ArrayList<>();
        for (List<SerieEntrenamiento> series : e.getBloques()) {
            Map<String, Object> bloque = bloque(series.get(0).getEjercicio(), null);
            List<Map<String, Object>> filas = new ArrayList<>();
            for (SerieEntrenamiento s : series) {
                filas.add(Map.of("peso", s.getPeso(), "reps", s.getRepeticiones(), "hecha", true));
            }
            bloque.put("series", filas);
            bloques.add(bloque);
        }
        return formulario(datos, bloques, entrenamientoService.ultimaVez(usuarioId, id), usuarioId, model, true);
    }

    @PostMapping("/entrenamiento/guardar")
    public String guardar(@ModelAttribute EntrenamientoForm form, RedirectAttributes redirect) {
        try {
            Entrenamiento guardado = entrenamientoService.guardar(form, UsuarioActual.id());
            return "redirect:/historial/" + guardado.getId() + "?guardado";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            if (form.getId() != null) {
                return "redirect:/historial/" + form.getId() + "/editar";
            }
            return "redirect:/entrenamiento/nuevo" + (form.getRutinaId() != null ? "?rutina=" + form.getRutinaId() : "");
        }
    }

    // ---------- Historial ----------

    @GetMapping("/historial")
    public String historial(Model model) {
        Map<String, List<Entrenamiento>> porMes = new LinkedHashMap<>();
        for (Entrenamiento e : entrenamientoService.historial(UsuarioActual.id())) {
            porMes.computeIfAbsent(fmt.mes(e.getFecha()), k -> new ArrayList<>()).add(e);
        }
        model.addAttribute("porMes", porMes);
        return "historial";
    }

    @GetMapping("/historial/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Entrenamiento e = entrenamientoService.obtener(id, UsuarioActual.id());
        model.addAttribute("entrenamiento", e);
        model.addAttribute("records", entrenamientoService.records(e));
        return "detalle-entrenamiento";
    }

    @PostMapping("/historial/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        entrenamientoService.eliminar(id, UsuarioActual.id());
        redirect.addFlashAttribute("exito", "Entrenamiento eliminado.");
        return "redirect:/historial";
    }

    // ---------- Progreso ----------

    @GetMapping("/progreso")
    public String progreso(Model model) {
        model.addAttribute("progresos", entrenamientoService.progreso(UsuarioActual.id()));
        return "progreso";
    }

    @GetMapping("/progreso/{ejercicioId}")
    public String progresoEjercicio(@PathVariable Long ejercicioId, Model model) {
        ProgresoEjercicio progreso = entrenamientoService.progresoDe(ejercicioId, UsuarioActual.id());
        List<Map<String, Object>> puntos = new ArrayList<>();
        for (ProgresoEjercicio.Sesion s : progreso.sesiones()) {
            puntos.add(Map.of(
                "fecha", s.fecha().toString(),
                "etiqueta", fmt.fecha(s.fecha()),
                "unoRm", Math.round(s.unoRm() * 10) / 10.0,
                "mejorPeso", s.mejorPeso()));
        }
        model.addAttribute("progreso", progreso);
        model.addAttribute("puntosJson", JSON.writeValueAsString(puntos));
        return "progreso-ejercicio";
    }

    // ---------- Ayudas ----------

    private String formulario(Map<String, Object> datos, List<Map<String, Object>> bloques,
                              Map<Long, List<SerieEntrenamiento>> ultimaVez, UUID usuarioId,
                              Model model, boolean editando) {
        datos.put("bloques", bloques);

        List<Map<String, Object>> catalogo = new ArrayList<>();
        for (Ejercicio e : ejercicioService.obtenerVisibles(usuarioId)) {
            catalogo.add(Map.of("id", e.getId(), "nombre", e.getNombre(),
                "grupo", e.getGrupoMuscular() != null ? e.getGrupoMuscular() : "Otros"));
        }
        datos.put("catalogo", catalogo);

        Map<String, Object> ultima = new LinkedHashMap<>();
        ultimaVez.forEach((ejercicioId, series) -> {
            List<Map<String, Object>> filas = new ArrayList<>();
            for (SerieEntrenamiento s : series) {
                filas.add(Map.of("peso", s.getPeso(), "reps", s.getRepeticiones()));
            }
            LocalDate fecha = series.get(0).getEntrenamiento().getFecha();
            ultima.put(ejercicioId.toString(), Map.of("fecha", fmt.relativa(fecha), "series", filas));
        });
        datos.put("ultimaVez", ultima);

        model.addAttribute("datosJson", JSON.writeValueAsString(datos));
        model.addAttribute("editando", editando);
        model.addAttribute("titulo", datos.get("nombre"));
        return "entrenar";
    }

    private static Map<String, Object> bloque(Ejercicio ejercicio, Integer descanso) {
        Map<String, Object> bloque = new LinkedHashMap<>();
        bloque.put("ejercicioId", ejercicio.getId());
        bloque.put("nombre", ejercicio.getNombre());
        bloque.put("grupo", ejercicio.getGrupoMuscular() != null ? ejercicio.getGrupoMuscular() : "");
        bloque.put("descanso", descanso != null ? descanso : 90);
        return bloque;
    }
}
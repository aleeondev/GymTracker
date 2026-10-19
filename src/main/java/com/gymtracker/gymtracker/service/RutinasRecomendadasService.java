package com.gymtracker.gymtracker.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.model.Ejercicio;
import com.gymtracker.gymtracker.model.Perfil;
import com.gymtracker.gymtracker.model.Rutina;
import com.gymtracker.gymtracker.model.RutinaEjercicio;
import com.gymtracker.gymtracker.repository.EjercicioRepository;

/**
 * Planes de rutinas ya montados (Full body, Torso/Pierna, Empuje/Tirón/Pierna).
 *  - Cuál se recomienda depende de los días por semana del cuestionario.
 *  - Series, repeticiones y descanso dependen del objetivo y la experiencia.
 * Los ejercicios son de la biblioteca común (InicializacionBaseDatos): si cambias
 * un nombre allí, cámbialo también aquí.
 */
@Service
public class RutinasRecomendadasService {

    /** Una rutina del plan: nombre y ejercicios (por nombre de la biblioteca). */
    public record RutinaPlan(String nombre, List<String> ejercicios) {
    }

    /** Un plan completo. */
    public record Plan(String clave, String nombre, String dias, String descripcion, List<RutinaPlan> rutinas) {
    }

    /** Cuántas series, repeticiones y descanso según tu objetivo. */
    public record Esquema(int series, int repeticiones, int descanso, String explicacion) {
    }

    public static final List<Plan> PLANES = List.of(
        new Plan("full_body", "Full body", "2 - 3 días",
            "Todo el cuerpo en cada sesión. Alterna A y B: ideal si entrenas pocos días o estás empezando.",
            List.of(
                new RutinaPlan("Full body A", List.of("Sentadilla", "Press banca", "Remo con barra",
                    "Press de hombros con mancuernas", "Curl femoral tumbado", "Plancha")),
                new RutinaPlan("Full body B", List.of("Peso muerto rumano", "Press inclinado con mancuernas",
                    "Jalón al pecho", "Prensa de piernas", "Elevaciones laterales", "Curl con mancuernas")))),
        new Plan("torso_pierna", "Torso / Pierna", "4 días",
            "Un día tren superior y otro tren inferior, dos veces por semana cada uno.",
            List.of(
                new RutinaPlan("Torso", List.of("Press banca", "Remo con barra", "Press de hombros con mancuernas",
                    "Jalón al pecho", "Curl con barra", "Extensión de tríceps en polea")),
                new RutinaPlan("Pierna", List.of("Sentadilla", "Peso muerto rumano", "Prensa de piernas",
                    "Curl femoral tumbado", "Elevación de gemelos de pie", "Crunch en polea")))),
        new Plan("ppl", "Empuje / Tirón / Pierna", "5 - 6 días",
            "Empuje (pecho, hombro, tríceps), tirón (espalda, bíceps) y pierna. Repite el ciclo.",
            List.of(
                new RutinaPlan("Empuje", List.of("Press banca", "Press militar", "Press inclinado con mancuernas",
                    "Elevaciones laterales", "Extensión de tríceps en polea", "Fondos en paralelas")),
                new RutinaPlan("Tirón", List.of("Dominadas", "Remo con barra", "Remo en polea baja",
                    "Face pull", "Curl con barra", "Curl martillo")),
                new RutinaPlan("Pierna", List.of("Sentadilla", "Peso muerto rumano", "Prensa de piernas",
                    "Curl femoral tumbado", "Hip thrust", "Elevación de gemelos de pie")))));

    private final RutinaService rutinaService;
    private final EjercicioRepository ejercicioRepository;

    public RutinasRecomendadasService(RutinaService rutinaService, EjercicioRepository ejercicioRepository) {
        this.rutinaService = rutinaService;
        this.ejercicioRepository = ejercicioRepository;
    }

    /** Clave del plan que encaja con tus días por semana (o null si no lo sabemos). */
    public String recomendado(Perfil perfil) {
        Integer dias = perfil.getDiasSemana();
        if (dias == null) {
            return null;
        }
        return dias <= 3 ? "full_body" : dias == 4 ? "torso_pierna" : "ppl";
    }

    public Esquema esquema(Perfil perfil) {
        String objetivo = perfil.getObjetivo() == null ? "" : perfil.getObjetivo();
        Esquema base = switch (objetivo) {
            case "fuerza" -> new Esquema(5, 5, 180, "pocas repeticiones con mucho peso y descansos largos");
            case "ganar_musculo", "definir", "recomposicion" -> new Esquema(4, 10, 90, "el rango clásico para ganar músculo");
            case "perder_grasa", "salud" -> new Esquema(3, 15, 60, "más repeticiones y descansos cortos");
            default -> new Esquema(3, 10, 90, "un esquema equilibrado para empezar");
        };
        if ("empiezo".equals(perfil.getExperiencia()) && base.series() > 3) {
            return new Esquema(3, base.repeticiones(), base.descanso(), base.explicacion() + ", con menos series al empezar");
        }
        return base;
    }

    /**
     * Crea en tu cuenta las rutinas del plan, con sus ejercicios y el esquema de tu objetivo.
     * Si ya tienes una rutina con el mismo nombre, la nueva se llama "Torso (2)".
     * Devuelve los nombres de las rutinas creadas.
     */
    @Transactional
    public List<String> crear(String clave, Perfil perfil, UUID usuarioId) {
        Plan plan = PLANES.stream().filter(p -> p.clave().equals(clave)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Plan desconocido"));
        Esquema esquema = esquema(perfil);

        Map<String, Ejercicio> biblioteca = new LinkedHashMap<>();
        for (Ejercicio e : ejercicioRepository.findByUsuarioIdIsNull()) {
            biblioteca.put(e.getNombre().toLowerCase(Locale.ROOT), e);
        }
        Set<String> nombresUsados = new HashSet<>();
        rutinaService.obtenerDelUsuario(usuarioId).forEach(r -> nombresUsados.add(r.getNombre()));

        List<String> creadas = new ArrayList<>();
        for (RutinaPlan rp : plan.rutinas()) {
            Rutina datos = new Rutina();
            datos.setNombre(nombreLibre(rp.nombre(), nombresUsados));
            datos.setDescripcion("Rutina recomendada: " + plan.nombre() + " (" + plan.dias() + "). " + plan.descripcion());
            Rutina rutina = rutinaService.guardar(datos, usuarioId);
            nombresUsados.add(rutina.getNombre());

            for (String nombre : rp.ejercicios()) {
                Ejercicio ejercicio = biblioteca.get(nombre.toLowerCase(Locale.ROOT));
                if (ejercicio == null) {
                    continue; // no está en la biblioteca: lo saltamos
                }
                RutinaEjercicio re = new RutinaEjercicio();
                re.setSeries(esquema.series());
                re.setRepeticiones(esquema.repeticiones());
                re.setDescanso(esquema.descanso());
                rutinaService.guardarEjercicio(rutina.getId(), re, ejercicio.getId(), usuarioId);
            }
            creadas.add(rutina.getNombre());
        }
        return creadas;
    }

    private static String nombreLibre(String base, Set<String> usados) {
        String nombre = base;
        for (int n = 2; usados.contains(nombre); n++) {
            nombre = base + " (" + n + ")";
        }
        return nombre;
    }
}

package com.gymtracker.gymtracker.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.dto.Logro;
import com.gymtracker.gymtracker.dto.ProgresoEjercicio;
import com.gymtracker.gymtracker.dto.ResumenLogros;
import com.gymtracker.gymtracker.model.Entrenamiento;
import com.gymtracker.gymtracker.model.PesoCorporal;
import com.gymtracker.gymtracker.repository.PesoCorporalRepository;
import com.gymtracker.gymtracker.repository.RutinaRepository;

/**
 * "Mis logros": medallas por hitos (entrenos, rachas, récords, volumen...).
 *
 * No guardamos nada nuevo en la base de datos: cada logro se CALCULA a partir
 * de los datos que ya tenemos (entrenos, récords, rutinas, pesos). Así no hace
 * falta tabla nueva ni migración, y si borras un entreno el logro se recalcula.
 * (Cuando lleguen las temporadas y los retos, sí tendrán su propia tabla.)
 *
 * Para añadir un logro: una línea más en logros(). Si usa un dato nuevo,
 * se calcula arriba, junto a los demás.
 */
@Service
public class LogrosService {

    private final EntrenamientoService entrenamientoService;
    private final RutinaRepository rutinaRepository;
    private final PesoCorporalRepository pesoCorporalRepository;

    public LogrosService(EntrenamientoService entrenamientoService, RutinaRepository rutinaRepository,
                         PesoCorporalRepository pesoCorporalRepository) {
        this.entrenamientoService = entrenamientoService;
        this.rutinaRepository = rutinaRepository;
        this.pesoCorporalRepository = pesoCorporalRepository;
    }

    @Transactional(readOnly = true)
    public ResumenLogros logros(UUID usuarioId) {
        LocalDate hoy = EntrenamientoService.hoy();

        // ---------- 1) Entrenos, del más antiguo al más nuevo (sin fechas futuras) ----------
        List<Entrenamiento> entrenos = new ArrayList<>(entrenamientoService.historial(usuarioId));
        entrenos.removeIf(e -> e.getFecha().isAfter(hoy));
        entrenos.sort(Comparator.comparing(Entrenamiento::getFecha));
        List<LocalDate> fechasEntrenos = entrenos.stream().map(Entrenamiento::getFecha).toList();

        // ---------- 2) Volumen acumulado: el día en que se cruza cada umbral ----------
        double volumenTotal = 0;
        Map<Long, LocalDate> cruceVolumen = new HashMap<>();
        for (Entrenamiento e : entrenos) {
            volumenTotal += e.getVolumen();
            for (long umbral : new long[] { 10_000, 100_000 }) {
                if (volumenTotal >= umbral) {
                    cruceVolumen.putIfAbsent(umbral, e.getFecha());
                }
            }
        }

        // ---------- 3) Récords batidos y ejercicios distintos (a partir del progreso) ----------
        // Mismo criterio que la página de Récords: superar tu mejor peso anterior
        // en ese ejercicio (la primera sesión de cada ejercicio no cuenta).
        List<LocalDate> fechasRecords = new ArrayList<>();
        List<LocalDate> fechasEjerciciosNuevos = new ArrayList<>();
        for (ProgresoEjercicio p : entrenamientoService.progreso(usuarioId)) {
            fechasEjerciciosNuevos.add(p.sesiones().get(0).fecha());
            double mejor = p.sesiones().get(0).mejorPeso();
            for (ProgresoEjercicio.Sesion s : p.sesiones().subList(1, p.sesiones().size())) {
                if (s.mejorPeso() > mejor) {
                    fechasRecords.add(s.fecha());
                    mejor = s.mejorPeso();
                }
            }
        }
        fechasRecords.sort(null);
        fechasEjerciciosNuevos.sort(null);

        // ---------- 4) Mejor racha: semanas seguidas con al menos un entreno ----------
        TreeMap<LocalDate, LocalDate> semanas = new TreeMap<>(); // lunes -> primer entreno de esa semana
        fechasEntrenos.forEach(f -> semanas.putIfAbsent(f.with(DayOfWeek.MONDAY), f));
        int racha = 0;
        int mejorRacha = 0;
        LocalDate lunesAnterior = null;
        Map<Integer, LocalDate> llegadaARacha = new HashMap<>(); // nº de semanas -> día en que la alcanzaste
        for (Map.Entry<LocalDate, LocalDate> semana : semanas.entrySet()) {
            boolean seguida = lunesAnterior != null && semana.getKey().equals(lunesAnterior.plusWeeks(1));
            racha = seguida ? racha + 1 : 1;
            lunesAnterior = semana.getKey();
            if (racha > mejorRacha) {
                mejorRacha = racha;
                llegadaARacha.put(racha, semana.getValue());
            }
        }

        // ---------- 5) Hábitos ----------
        int rutinas = rutinaRepository.findByUsuarioIdOrderByNombreAsc(usuarioId).size();
        List<LocalDate> fechasPeso = pesoCorporalRepository.findByUsuarioIdOrderByFechaAsc(usuarioId).stream()
            .map(PesoCorporal::getFecha).toList();

        // ---------- Las medallas ----------
        List<Logro> logros = new ArrayList<>();

        String c = "Constancia";
        logros.add(porFechas("primer-entreno", c, "Primer paso", "Completa tu primer entrenamiento.",
            "pesa", "bronce", 1, fechasEntrenos, "entreno"));
        logros.add(porFechas("entrenos-10", c, "Diez de diez", "Completa 10 entrenamientos.",
            "pesa", "bronce", 10, fechasEntrenos, "entrenos"));
        logros.add(porFechas("entrenos-50", c, "Medio centenar", "Completa 50 entrenamientos.",
            "pesa", "plata", 50, fechasEntrenos, "entrenos"));
        logros.add(porFechas("entrenos-100", c, "Centurión", "Completa 100 entrenamientos.",
            "pesa", "oro", 100, fechasEntrenos, "entrenos"));
        logros.add(new Logro("racha-4", c, "Un mes sin fallar",
            "Entrena al menos una vez por semana durante 4 semanas seguidas.",
            "calendario", "plata", 4, mejorRacha, "semanas", llegadaARacha.get(4)));
        logros.add(new Logro("racha-12", c, "Imparable", "12 semanas seguidas entrenando.",
            "calendario", "oro", 12, mejorRacha, "semanas", llegadaARacha.get(12)));

        String f = "Fuerza";
        logros.add(porFechas("primer-record", f, "Primer récord", "Supera por primera vez tu mejor peso en un ejercicio.",
            "trofeo", "bronce", 1, fechasRecords, "récord"));
        logros.add(porFechas("records-10", f, "Cazarrécords", "Bate 10 récords personales.",
            "trofeo", "plata", 10, fechasRecords, "récords"));
        logros.add(porFechas("records-50", f, "Leyenda del gimnasio", "Bate 50 récords personales.",
            "trofeo", "oro", 50, fechasRecords, "récords"));
        logros.add(new Logro("volumen-10t", f, "Diez toneladas",
            "Mueve 10.000 kg en total (peso × repeticiones de todas tus series).",
            "grafica", "plata", 10_000, Math.round(volumenTotal), "kg", cruceVolumen.get(10_000L)));
        logros.add(new Logro("volumen-100t", f, "Cien toneladas", "Mueve 100.000 kg en total.",
            "grafica", "oro", 100_000, Math.round(volumenTotal), "kg", cruceVolumen.get(100_000L)));

        String h = "Hábitos";
        logros.add(new Logro("primera-rutina", h, "Con un plan", "Crea tu primera rutina.",
            "lista", "bronce", 1, rutinas, "rutina", null));
        logros.add(porFechas("explorador", h, "Explorador", "Entrena 10 ejercicios distintos.",
            "destello", "bronce", 10, fechasEjerciciosNuevos, "ejercicios"));
        logros.add(porFechas("bascula", h, "Báscula al día", "Registra tu peso corporal 4 veces.",
            "bascula", "bronce", 4, fechasPeso, "registros"));

        return new ResumenLogros(logros);
    }

    /**
     * Logro de "conseguir N veces algo" a partir de las fechas en que pasó (ordenadas).
     * Se consigue el día de la vez número N.
     */
    private static Logro porFechas(String id, String categoria, String nombre, String descripcion, String icono,
                                   String nivel, int objetivo, List<LocalDate> fechas, String unidad) {
        LocalDate conseguido = fechas.size() >= objetivo ? fechas.get(objetivo - 1) : null;
        return new Logro(id, categoria, nombre, descripcion, icono, nivel, objetivo, fechas.size(), unidad, conseguido);
    }
}

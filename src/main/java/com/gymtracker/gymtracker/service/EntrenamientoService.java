package com.gymtracker.gymtracker.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gymtracker.gymtracker.dto.EntrenamientoForm;
import com.gymtracker.gymtracker.dto.ProgresoEjercicio;
import com.gymtracker.gymtracker.dto.ResumenDashboard;
import com.gymtracker.gymtracker.dto.SemanaVolumen;
import com.gymtracker.gymtracker.model.Ejercicio;
import com.gymtracker.gymtracker.model.Entrenamiento;
import com.gymtracker.gymtracker.model.Rutina;
import com.gymtracker.gymtracker.model.SerieEntrenamiento;
import com.gymtracker.gymtracker.repository.EntrenamientoRepository;
import com.gymtracker.gymtracker.repository.SerieEntrenamientoRepository;

@Service
public class EntrenamientoService {

    /** Zona horaria con la que se decide qué es "hoy" o "esta semana". */
    public static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    private static final int MAX_REPETICIONES = 1000;
    private static final double MAX_PESO = 1500;

    private final EntrenamientoRepository entrenamientoRepository;
    private final SerieEntrenamientoRepository serieRepository;
    private final RutinaService rutinaService;
    private final EjercicioService ejercicioService;

    public EntrenamientoService(EntrenamientoRepository entrenamientoRepository,
                                SerieEntrenamientoRepository serieRepository,
                                RutinaService rutinaService, EjercicioService ejercicioService) {
        this.entrenamientoRepository = entrenamientoRepository;
        this.serieRepository = serieRepository;
        this.rutinaService = rutinaService;
        this.ejercicioService = ejercicioService;
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public List<Entrenamiento> historial(UUID usuarioId) {
        return entrenamientoRepository.findByUsuarioIdOrderByFechaDescCreadoEnDesc(usuarioId);
    }

    public Entrenamiento obtener(Long id, UUID usuarioId) {
        return entrenamientoRepository.findByIdAndUsuarioId(id, usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * Crea (o actualiza, si form.id viene relleno) un entrenamiento.
     * Las filas sin repeticiones se ignoran: son series que no se llegaron a hacer.
     */
    @Transactional
    public Entrenamiento guardar(EntrenamientoForm form, UUID usuarioId) {
        Entrenamiento entrenamiento = form.getId() != null ? obtener(form.getId(), usuarioId) : new Entrenamiento();
        entrenamiento.setUsuarioId(usuarioId);

        Rutina rutina = form.getRutinaId() != null ? rutinaService.obtener(form.getRutinaId(), usuarioId) : null;
        if (rutina != null || form.getId() == null) {
            entrenamiento.setRutina(rutina);
        }

        String nombre = EjercicioService.limpiar(form.getNombre());
        if (nombre == null) {
            nombre = rutina != null ? rutina.getNombre() : "Entrenamiento libre";
        }
        entrenamiento.setNombre(recortar(nombre, 120));
        entrenamiento.setFecha(form.getFecha() != null ? form.getFecha() : hoy());

        Integer duracion = form.getDuracionMinutos();
        entrenamiento.setDuracionMinutos(duracion != null && duracion > 0 && duracion <= 600 ? duracion : null);
        String notas = EjercicioService.limpiar(form.getNotas());
        entrenamiento.setNotas(notas != null ? recortar(notas, 1000) : null);

        List<EntrenamientoForm.SerieForm> validas = new ArrayList<>();
        for (EntrenamientoForm.SerieForm serie : form.getSeries()) {
            if (serie == null || serie.getEjercicioId() == null || serie.getRepeticiones() == null) {
                continue;
            }
            if (serie.getRepeticiones() <= 0 || serie.getRepeticiones() > MAX_REPETICIONES) {
                continue;
            }
            Double peso = serie.getPeso();
            if (peso != null && (peso.isNaN() || peso < 0 || peso > MAX_PESO)) {
                continue;
            }
            validas.add(serie);
        }
        if (validas.isEmpty()) {
            throw new IllegalArgumentException("Apunta al menos una serie con repeticiones para guardar el entrenamiento.");
        }
        // Orden estable por bloque: respeta el orden de las series dentro de cada ejercicio.
        validas.sort(Comparator.comparing(s -> s.getBloque() != null ? s.getBloque() : Integer.MAX_VALUE));

        entrenamiento.getSeries().clear();
        Map<Long, Ejercicio> ejercicios = new HashMap<>();
        Integer bloqueAnterior = null;
        int orden = 0;
        int numero = 0;
        for (EntrenamientoForm.SerieForm datos : validas) {
            if (orden == 0 || !Objects.equals(datos.getBloque(), bloqueAnterior)) {
                orden++;
                numero = 0;
                bloqueAnterior = datos.getBloque();
            }
            numero++;

            SerieEntrenamiento serie = new SerieEntrenamiento();
            serie.setEntrenamiento(entrenamiento);
            serie.setEjercicio(ejercicios.computeIfAbsent(datos.getEjercicioId(),
                id -> ejercicioService.obtenerVisible(id, usuarioId)));
            serie.setOrden(orden);
            serie.setNumeroSerie(numero);
            serie.setRepeticiones(datos.getRepeticiones());
            serie.setPeso(datos.getPeso() != null ? Math.round(datos.getPeso() * 100) / 100.0 : 0.0);
            entrenamiento.getSeries().add(serie);
        }
        return entrenamientoRepository.save(entrenamiento);
    }

    @Transactional
    public void eliminar(Long id, UUID usuarioId) {
        entrenamientoRepository.delete(obtener(id, usuarioId));
    }

    // ---------- Estadísticas ----------

    @Transactional(readOnly = true)
    /**
     * Volumen de cada una de las últimas {@code semanas} semanas, de la más antigua
     * a la actual. Las semanas sin entrenos también aparecen (con 0), para que la
     * gráfica muestre los huecos.
     */
    public List<SemanaVolumen> volumenSemanal(UUID usuarioId, int semanas) {
        LocalDate lunesActual = hoy().with(DayOfWeek.MONDAY);
        LocalDate desde = lunesActual.minusWeeks(semanas - 1L);

        // Una "hucha" por semana: [volumen, nº de entrenos]
        Map<LocalDate, double[]> porSemana = new LinkedHashMap<>();
        for (int i = 0; i < semanas; i++) {
            porSemana.put(desde.plusWeeks(i), new double[2]);
        }
        for (Entrenamiento e : entrenamientoRepository.findByUsuarioIdAndFechaGreaterThanEqualOrderByFechaDesc(usuarioId, desde)) {
            double[] hucha = porSemana.get(e.getFecha().with(DayOfWeek.MONDAY));
            if (hucha != null) { // null = fecha futura, fuera de la gráfica
                hucha[0] += e.getVolumen();
                hucha[1]++;
            }
        }

        List<SemanaVolumen> resultado = new ArrayList<>();
        porSemana.forEach((lunes, hucha) -> resultado.add(new SemanaVolumen(lunes, hucha[0], (int) hucha[1])));
        return resultado;
    }

    /**
     * Cuántas series has hecho de cada grupo muscular en los últimos {@code dias} días,
     * de más a menos. Incluye los grupos con 0 series (es justo lo que interesa ver),
     * salvo Cardio, que solo aparece si lo has hecho. Ejercicios sin grupo: "Otros".
     */
    public Map<String, Integer> seriesPorGrupo(UUID usuarioId, int dias) {
        LocalDate hoy = hoy();
        LocalDate desde = hoy.minusDays(dias - 1L);

        Map<String, Integer> cuenta = new HashMap<>();
        for (String grupo : EjercicioService.GRUPOS_MUSCULARES) {
            if (!grupo.equals("Cardio")) {
                cuenta.put(grupo, 0);
            }
        }
        for (Entrenamiento e : entrenamientoRepository.findByUsuarioIdAndFechaGreaterThanEqualOrderByFechaDesc(usuarioId, desde)) {
            if (e.getFecha().isAfter(hoy)) {
                continue;
            }
            for (SerieEntrenamiento s : e.getSeries()) {
                String grupo = s.getEjercicio().getGrupoMuscular();
                cuenta.merge(grupo == null || grupo.isBlank() ? "Otros" : grupo, 1, Integer::sum);
            }
        }

        // De más series a menos; a igualdad, en el orden habitual de los grupos
        List<String> orden = new ArrayList<>(EjercicioService.GRUPOS_MUSCULARES);
        orden.add("Otros");
        Map<String, Integer> resultado = new LinkedHashMap<>();
        cuenta.entrySet().stream()
            .sorted(Comparator.comparing((Map.Entry<String, Integer> g) -> -g.getValue())
                .thenComparing(g -> orden.indexOf(g.getKey()) < 0 ? Integer.MAX_VALUE : orden.indexOf(g.getKey())))
            .forEach(g -> resultado.put(g.getKey(), g.getValue()));
        return resultado;
    }

    public ResumenDashboard resumen(UUID usuarioId) {
        List<Entrenamiento> todos = historial(usuarioId);
        LocalDate hoy = hoy();
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        long semana = 0;
        long mes = 0;
        double volumenSemana = 0;
        Set<LocalDate> semanasConEntreno = new HashSet<>();
        for (Entrenamiento e : todos) {
            semanasConEntreno.add(e.getFecha().with(DayOfWeek.MONDAY));
            if (!e.getFecha().isBefore(lunes) && !e.getFecha().isAfter(hoy)) {
                semana++;
                volumenSemana += e.getVolumen();
            }
            if (!e.getFecha().isBefore(inicioMes) && !e.getFecha().isAfter(hoy)) {
                mes++;
            }
        }

        // Racha: semanas seguidas con al menos un entreno. La semana actual
        // no rompe la racha aunque todavía no hayas entrenado.
        int racha = 0;
        LocalDate semanaRevisada = semanasConEntreno.contains(lunes) ? lunes : lunes.minusWeeks(1);
        while (semanasConEntreno.contains(semanaRevisada)) {
            racha++;
            semanaRevisada = semanaRevisada.minusWeeks(1);
        }

        List<Entrenamiento> ultimos = new ArrayList<>(todos.subList(0, Math.min(3, todos.size())));
        ultimos.forEach(e -> e.getSeries().size());
        return new ResumenDashboard(semana, mes, volumenSemana, racha, todos.size(), ultimos);
    }

    /** Progreso de todos los ejercicios entrenados, el más reciente primero. */
    @Transactional(readOnly = true)
    public List<ProgresoEjercicio> progreso(UUID usuarioId) {
        Map<Long, List<SerieEntrenamiento>> porEjercicio = new LinkedHashMap<>();
        for (SerieEntrenamiento serie : serieRepository.findDelUsuario(usuarioId)) {
            porEjercicio.computeIfAbsent(serie.getEjercicio().getId(), k -> new ArrayList<>()).add(serie);
        }
        List<ProgresoEjercicio> resultado = new ArrayList<>();
        porEjercicio.values().forEach(series -> resultado.add(construirProgreso(series)));
        return resultado;
    }

    @Transactional(readOnly = true)
    public ProgresoEjercicio progresoDe(Long ejercicioId, UUID usuarioId) {
        return progreso(usuarioId).stream()
            .filter(p -> p.ejercicio().getId().equals(ejercicioId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * Series de la última vez que se hizo cada ejercicio (se muestran como
     * referencia al registrar un entrenamiento nuevo).
     */
    @Transactional(readOnly = true)
    public Map<Long, List<SerieEntrenamiento>> ultimaVez(UUID usuarioId, Long excluirEntrenamientoId) {
        Map<Long, Long> sesionPorEjercicio = new HashMap<>();
        Map<Long, List<SerieEntrenamiento>> resultado = new LinkedHashMap<>();
        for (SerieEntrenamiento serie : serieRepository.findDelUsuario(usuarioId)) {
            Long entrenamientoId = serie.getEntrenamiento().getId();
            if (entrenamientoId.equals(excluirEntrenamientoId)) {
                continue;
            }
            Long ejercicioId = serie.getEjercicio().getId();
            Long sesion = sesionPorEjercicio.putIfAbsent(ejercicioId, entrenamientoId);
            if (sesion == null || sesion.equals(entrenamientoId)) {
                resultado.computeIfAbsent(ejercicioId, k -> new ArrayList<>()).add(serie);
            }
        }
        return resultado;
    }

    /** Ejercicios de este entrenamiento en los que se superó el mejor 1RM estimado anterior. */
    @Transactional(readOnly = true)
    public Set<Long> records(Entrenamiento entrenamiento) {
        Map<Long, Double> mejorAnterior = new HashMap<>();
        for (SerieEntrenamiento serie : serieRepository.findDelUsuario(entrenamiento.getUsuarioId())) {
            Entrenamiento otro = serie.getEntrenamiento();
            if (otro.getId().equals(entrenamiento.getId())) {
                continue;
            }
            boolean anterior = otro.getFecha().isBefore(entrenamiento.getFecha())
                || (otro.getFecha().isEqual(entrenamiento.getFecha()) && otro.getCreadoEn().isBefore(entrenamiento.getCreadoEn()));
            if (anterior) {
                mejorAnterior.merge(serie.getEjercicio().getId(), serie.getUnoRmEstimado(), Math::max);
            }
        }
        Set<Long> records = new HashSet<>();
        for (SerieEntrenamiento serie : entrenamiento.getSeries()) {
            Double previo = mejorAnterior.get(serie.getEjercicio().getId());
            if (previo != null && serie.getUnoRmEstimado() > previo + 0.01) {
                records.add(serie.getEjercicio().getId());
            }
        }
        return records;
    }

    private static ProgresoEjercicio construirProgreso(List<SerieEntrenamiento> series) {
        Map<Long, List<SerieEntrenamiento>> porSesion = new LinkedHashMap<>();
        for (SerieEntrenamiento serie : series) {
            porSesion.computeIfAbsent(serie.getEntrenamiento().getId(), k -> new ArrayList<>()).add(serie);
        }
        List<ProgresoEjercicio.Sesion> sesiones = new ArrayList<>();
        for (List<SerieEntrenamiento> sesion : porSesion.values()) {
            SerieEntrenamiento mejor = sesion.stream()
                .max(Comparator.comparingDouble(SerieEntrenamiento::getPeso)
                    .thenComparingInt(SerieEntrenamiento::getRepeticiones))
                .orElseThrow();
            sesiones.add(new ProgresoEjercicio.Sesion(
                mejor.getEntrenamiento().getId(),
                mejor.getEntrenamiento().getFecha(),
                sesion.size(),
                mejor.getPeso(),
                mejor.getRepeticiones(),
                sesion.stream().mapToDouble(SerieEntrenamiento::getUnoRmEstimado).max().orElse(0),
                sesion.stream().mapToDouble(SerieEntrenamiento::getVolumen).sum()));
        }
        Collections.reverse(sesiones);
        return new ProgresoEjercicio(series.get(0).getEjercicio(), sesiones);
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
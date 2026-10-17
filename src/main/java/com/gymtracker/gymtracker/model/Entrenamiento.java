package com.gymtracker.gymtracker.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Una sesión de entrenamiento ya realizada (lo que forma el historial). */
@Entity
@Table(name = "entrenamientos")
@Getter
@Setter
public class Entrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /** Rutina de la que salió (puede ser null si fue libre o si se borró la rutina). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;

    /** Copia del nombre para que el historial no cambie aunque se borre o renombre la rutina. */
    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fecha;

    private Integer duracionMinutos;

    @Column(length = 1000)
    private String notas;

    @Column(nullable = false)
    private Instant creadoEn = Instant.now();

    @OneToMany(mappedBy = "entrenamiento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, numeroSerie ASC")
    private List<SerieEntrenamiento> series = new ArrayList<>();

    /** Kg totales movidos (peso × repeticiones de todas las series). */
    public double getVolumen() {
        return series.stream().mapToDouble(SerieEntrenamiento::getVolumen).sum();
    }

    /** Series agrupadas por bloque de ejercicio, en el orden en que se hicieron. */
    public List<List<SerieEntrenamiento>> getBloques() {
        Map<Integer, List<SerieEntrenamiento>> bloques = new LinkedHashMap<>();
        for (SerieEntrenamiento serie : series) {
            bloques.computeIfAbsent(serie.getOrden(), k -> new ArrayList<>()).add(serie);
        }
        return new ArrayList<>(bloques.values());
    }
}

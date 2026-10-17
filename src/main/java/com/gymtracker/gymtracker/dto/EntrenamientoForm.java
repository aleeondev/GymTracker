package com.gymtracker.gymtracker.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** Lo que envía el formulario de "registrar entrenamiento". */
@Getter
@Setter
public class EntrenamientoForm {

    /** Solo al editar un entrenamiento ya guardado. */
    private Long id;
    private Long rutinaId;
    private String nombre;
    private LocalDate fecha;
    private Integer duracionMinutos;
    private String notas;
    private List<SerieForm> series = new ArrayList<>();

    @Getter
    @Setter
    public static class SerieForm {
        /** Posición del bloque de ejercicio en el formulario. */
        private Integer bloque;
        private Long ejercicioId;
        private Integer repeticiones;
        private Double peso;
    }
}

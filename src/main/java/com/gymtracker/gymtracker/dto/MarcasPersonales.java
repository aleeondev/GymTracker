package com.gymtracker.gymtracker.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.gymtracker.gymtracker.model.Ejercicio;

/**
 * Página "Mis récords".
 *
 * @param porGrupo       la mejor marca de cada ejercicio, agrupadas por grupo muscular
 * @param recientes      los últimos récords batidos (de más nuevo a más antiguo)
 * @param batidosEsteMes cuántos récords has batido este mes
 */
public record MarcasPersonales(Map<String, List<Marca>> porGrupo, List<RecordBatido> recientes, int batidosEsteMes) {

    /** Tu mejor marca en un ejercicio: el mayor peso levantado (la primera vez que lo lograste). */
    public record Marca(Ejercicio ejercicio, double peso, int reps, LocalDate fecha, Long entrenamientoId,
                        double mejorUnoRm, int sesiones) {
    }

    /** Un día en que superaste tu máximo anterior en un ejercicio. */
    public record RecordBatido(Ejercicio ejercicio, double peso, int reps, double anterior,
                               LocalDate fecha, Long entrenamientoId) {

        public double getMejora() {
            return Math.round((peso - anterior) * 100) / 100.0;
        }
    }

    public boolean isVacio() {
        return porGrupo.isEmpty();
    }

    public int getTotalEjercicios() {
        return porGrupo.values().stream().mapToInt(List::size).sum();
    }
}

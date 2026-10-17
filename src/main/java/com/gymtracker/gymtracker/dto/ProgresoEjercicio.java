package com.gymtracker.gymtracker.dto;

import java.time.LocalDate;
import java.util.List;

import com.gymtracker.gymtracker.model.Ejercicio;

/** Evolución de un ejercicio a lo largo de las sesiones (de la más antigua a la más reciente). */
public record ProgresoEjercicio(Ejercicio ejercicio, List<Sesion> sesiones) {

    /** Resumen de un ejercicio en un entrenamiento concreto. */
    public record Sesion(Long entrenamientoId, LocalDate fecha, int series, double mejorPeso,
                         int repsMejorPeso, double unoRm, double volumen) {
    }

    /** Las sesiones de la más reciente a la más antigua (para la tabla). */
    public List<Sesion> getSesionesRecientes() {
        return sesiones.reversed();
    }

    public Sesion getUltima() {
        return sesiones.get(sesiones.size() - 1);
    }

    public double getMejorPeso() {
        return sesiones.stream().mapToDouble(Sesion::mejorPeso).max().orElse(0);
    }

    public double getMejorUnoRm() {
        return sesiones.stream().mapToDouble(Sesion::unoRm).max().orElse(0);
    }

    /** % de mejora del 1RM estimado entre la primera y la última sesión. */
    public Double getMejoraPorcentaje() {
        double inicial = sesiones.get(0).unoRm();
        if (sesiones.size() < 2 || inicial <= 0) {
            return null;
        }
        return (getUltima().unoRm() - inicial) / inicial * 100;
    }
}

package com.gymtracker.gymtracker.dto;

import java.time.LocalDate;

/**
 * Volumen de una semana para la gráfica del panel.
 *
 * @param lunes    primer día de la semana (las semanas van de lunes a domingo)
 * @param volumen  kg movidos esa semana (suma de peso × repeticiones de todas las series)
 * @param entrenos cuántos entrenamientos hubo
 */
public record SemanaVolumen(LocalDate lunes, double volumen, int entrenos) {
}

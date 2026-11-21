package com.gymtracker.gymtracker.dto;

import java.time.LocalDate;

/**
 * Una medalla de "Mis logros".
 *
 * @param id           identificador fijo (sirve para tests y, en el futuro, para guardarlos)
 * @param categoria    sección donde aparece: Constancia, Fuerza, Hábitos
 * @param nombre       título corto: "Diez de diez"
 * @param descripcion  qué hay que hacer para conseguirla
 * @param icono        nombre de un icono de fragments/iconos.html
 * @param nivel        bronce, plata u oro (el color de la medalla)
 * @param objetivo     cuánto hace falta (10 entrenos, 4 semanas...)
 * @param actual       cuánto llevas (puede pasar del objetivo)
 * @param unidad       para el texto del progreso: "entrenos", "kg"...
 * @param conseguidoEn día en que la conseguiste (null si aún no, o si no se sabe)
 */
public record Logro(String id, String categoria, String nombre, String descripcion, String icono, String nivel,
                    long objetivo, long actual, String unidad, LocalDate conseguidoEn) {

    public boolean isConseguido() {
        return actual >= objetivo;
    }

    /** Lo que llevas, sin pasar del objetivo (para la barra de progreso). */
    public long getProgreso() {
        return Math.min(actual, objetivo);
    }

    /** Los mismos números como Double, para formatearlos en la vista con ${@fmt.entero(...)}. */
    public Double getProgresoNum() {
        return (double) getProgreso();
    }

    public Double getObjetivoNum() {
        return (double) objetivo;
    }
}

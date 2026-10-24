package com.gymtracker.gymtracker.dto;

import java.util.List;

/**
 * Tarjeta "Primeros pasos" del panel para cuentas nuevas: cuatro pasos que se
 * marcan solos según lo que ya ha hecho la persona. Cuando están todos, la
 * tarjeta deja de mostrarse.
 *
 * @param pasos en orden; cada uno sabe si está hecho
 */
public record PrimerosPasos(List<Paso> pasos) {

    /**
     * Un paso de la lista.
     *
     * @param titulo  "Crea tu primera rutina"
     * @param texto   explicación corta
     * @param url     a dónde lleva el botón; null = abre el diálogo "Opinar"
     * @param boton   texto del botón
     * @param hecho   si ya está completado
     */
    public record Paso(String titulo, String texto, String url, String boton, boolean hecho) {
    }

    public static PrimerosPasos de(boolean perfilCompleto, boolean tieneRutina, boolean haEntrenado, boolean haOpinado) {
        return new PrimerosPasos(List.of(
            new Paso("Completa tu perfil", "Tu objetivo, tu peso y tus días: el panel se adapta a ti.",
                "/bienvenida?editar", "Completar", perfilCompleto),
            new Paso("Crea tu primera rutina", "Hazla a tu medida o parte de una recomendada para tus días.",
                "/rutinas/recomendadas", "Elegir rutina", tieneRutina),
            new Paso("Haz tu primer entreno", "Apunta tus series y empezaremos a medir tu progreso.",
                "/entrenamiento", "Entrenar", haEntrenado),
            new Paso("Cuéntanos qué te parece", "Estás probando la versión alpha: tu opinión decide qué viene después.",
                null, "Opinar", haOpinado)));
    }

    public long hechos() {
        return pasos.stream().filter(Paso::hecho).count();
    }

    public int total() {
        return pasos.size();
    }

    public boolean completo() {
        return hechos() == total();
    }
}

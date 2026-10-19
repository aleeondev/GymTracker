package com.gymtracker.gymtracker.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Opciones de cada pregunta del cuestionario de bienvenida.
 * Clave = lo que se guarda en la base de datos; valor = lo que ve el usuario.
 * Para añadir una opción, añádela aquí: el formulario y la validación la usan solos.
 * (En las plantillas: ${T(com.gymtracker.gymtracker.dto.OpcionesPerfil).OBJETIVO})
 */
public final class OpcionesPerfil {

    private OpcionesPerfil() {
    }

    public static final Map<String, String> SEXO = mapa(
        "hombre", "Hombre",
        "mujer", "Mujer",
        "no_decir", "Prefiero no decirlo");

    public static final Map<String, String> OBJETIVO = mapa(
        "perder_grasa", "Perder grasa",
        "definir", "Definir",
        "ganar_musculo", "Ganar músculo",
        "recomposicion", "Recomposición",
        "fuerza", "Ganar fuerza",
        "salud", "Mantenerme y salud");

    public static final Map<String, String> OBJETIVO_DESCRIPCION = mapa(
        "perder_grasa", "Bajar de peso perdiendo grasa",
        "definir", "Marcar músculo perdiendo la grasa que lo tapa",
        "ganar_musculo", "Volumen: subir de peso ganando músculo",
        "recomposicion", "Perder grasa y ganar músculo a la vez",
        "fuerza", "Levantar más peso en los básicos",
        "salud", "Estar en forma y sentirme bien");

    public static final Map<String, String> EXPERIENCIA = mapa(
        "empiezo", "Empiezo ahora",
        "menos_1", "Menos de 1 año",
        "de_1_a_3", "1 - 3 años",
        "mas_3", "Más de 3 años");

    public static final Map<String, String> LUGAR = mapa(
        "gimnasio", "Gimnasio completo",
        "casa_material", "En casa con material",
        "casa_sin_material", "En casa sin material");

    public static final Map<String, String> ACTIVIDAD = mapa(
        "sedentario", "Sedentaria (sentado casi todo el día)",
        "ligero", "Ligera (camino algo)",
        "activo", "Activa (de pie o moviéndome mucho)",
        "muy_activo", "Muy activa (trabajo físico)");

    public static final Map<String, String> DURACION = mapa(
        "30", "30 min",
        "45", "45 min",
        "60", "1 hora",
        "90", "1 h 30 o más");

    public static final Map<String, String> DIAS = mapa(
        "2", "2", "3", "3", "4", "4", "5", "5", "6", "6");

    /** Todas juntas, para las plantillas: ${opcionesPerfil.objetivo['ganar_musculo']} */
    public static final Map<String, Map<String, String>> TODAS = Map.of(
        "sexo", SEXO, "objetivo", OBJETIVO, "objetivoDescripcion", OBJETIVO_DESCRIPCION,
        "experiencia", EXPERIENCIA, "lugar", LUGAR, "actividad", ACTIVIDAD,
        "duracion", DURACION, "dias", DIAS);

    private static Map<String, String> mapa(String... claveValor) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            m.put(claveValor[i], claveValor[i + 1]);
        }
        return Collections.unmodifiableMap(m); // mantiene el orden y no se puede modificar
    }
}

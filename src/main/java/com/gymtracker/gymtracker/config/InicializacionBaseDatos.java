package com.gymtracker.gymtracker.config;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.model.Ejercicio;
import com.gymtracker.gymtracker.repository.EjercicioRepository;

/**
 * Tareas al arrancar la aplicación:
 *
 * 1) Activa Row Level Security en nuestras tablas. La base de datos es la de
 *    Supabase, y Supabase publica automáticamente las tablas de "public" en
 *    su API REST. Sin RLS, cualquiera con la clave pública (la que va en
 *    supabase.js) podría leer o borrar datos. Con RLS activado y sin
 *    políticas, la API REST no puede tocarlas; Spring sí, porque se conecta
 *    con el usuario "postgres", que no está sujeto a RLS.
 *
 * 2) Crea los ejercicios de la biblioteca común que aún no existan.
 */
@Component
public class InicializacionBaseDatos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializacionBaseDatos.class);

    private static final List<String> TABLAS = List.of(
        "ejercicios", "rutinas", "rutina_ejercicios", "entrenamientos", "series_entrenamiento", "perfiles");

    private final JdbcTemplate jdbc;
    private final EjercicioRepository ejercicioRepository;

    public InicializacionBaseDatos(JdbcTemplate jdbc, EjercicioRepository ejercicioRepository) {
        this.jdbc = jdbc;
        this.ejercicioRepository = ejercicioRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        activarRls();
        crearBibliotecaComun();
    }

    private void activarRls() {
        for (String tabla : TABLAS) {
            try {
                jdbc.execute("alter table public." + tabla + " enable row level security");
            } catch (Exception e) {
                log.warn("No se pudo activar RLS en {}: {}", tabla, e.getMessage());
            }
        }
    }

    private void crearBibliotecaComun() {
        Set<String> existentes = new HashSet<>();
        ejercicioRepository.findByUsuarioIdIsNull()
            .forEach(e -> existentes.add(normalizar(e.getNombre())));
        String[][] biblioteca = {
            // nombre, grupo, equipamiento
            { "Press banca", "Pecho", "Barra" },
            { "Press inclinado con mancuernas", "Pecho", "Mancuernas" },
            { "Press banca en multipower", "Pecho", "Multipower" },
            { "Aperturas en polea", "Pecho", "Polea" },
            { "Press de pecho en máquina", "Pecho", "Máquina" },
            { "Fondos en paralelas", "Pecho", "Peso corporal" },
            { "Flexiones", "Pecho", "Peso corporal" },
            { "Dominadas", "Espalda", "Peso corporal" },
            { "Jalón al pecho", "Espalda", "Polea" },
            { "Remo con barra", "Espalda", "Barra" },
            { "Remo con mancuerna a una mano", "Espalda", "Mancuernas" },
            { "Remo en polea baja", "Espalda", "Polea" },
            { "Remo en máquina", "Espalda", "Máquina" },
            { "Peso muerto", "Espalda", "Barra" },
            { "Pullover en polea", "Espalda", "Polea" },
            { "Press militar", "Hombros", "Barra" },
            { "Press de hombros con mancuernas", "Hombros", "Mancuernas" },
            { "Elevaciones laterales", "Hombros", "Mancuernas" },
            { "Elevaciones laterales en polea", "Hombros", "Polea" },
            { "Pájaros (deltoides posterior)", "Hombros", "Mancuernas" },
            { "Face pull", "Hombros", "Polea" },
            { "Curl con barra", "Bíceps", "Barra" },
            { "Curl con mancuernas", "Bíceps", "Mancuernas" },
            { "Curl martillo", "Bíceps", "Mancuernas" },
            { "Curl en banco Scott", "Bíceps", "Máquina" },
            { "Curl en polea", "Bíceps", "Polea" },
            { "Extensión de tríceps en polea", "Tríceps", "Polea" },
            { "Press francés", "Tríceps", "Barra" },
            { "Extensión de tríceps sobre la cabeza", "Tríceps", "Mancuernas" },
            { "Fondos en banco", "Tríceps", "Peso corporal" },
            { "Press banca agarre cerrado", "Tríceps", "Barra" },
            { "Curl de muñeca", "Antebrazo", "Barra" },
            { "Sentadilla", "Cuádriceps", "Barra" },
            { "Sentadilla en multipower", "Cuádriceps", "Multipower" },
            { "Prensa de piernas", "Cuádriceps", "Máquina" },
            { "Extensión de cuádriceps", "Cuádriceps", "Máquina" },
            { "Zancadas", "Cuádriceps", "Mancuernas" },
            { "Sentadilla búlgara", "Cuádriceps", "Mancuernas" },
            { "Sentadilla hack", "Cuádriceps", "Máquina" },
            { "Peso muerto rumano", "Femoral", "Barra" },
            { "Curl femoral tumbado", "Femoral", "Máquina" },
            { "Curl femoral sentado", "Femoral", "Máquina" },
            { "Hip thrust", "Glúteos", "Barra" },
            { "Patada de glúteo en polea", "Glúteos", "Polea" },
            { "Abducción de cadera en máquina", "Glúteos", "Máquina" },
            { "Elevación de gemelos de pie", "Gemelos", "Máquina" },
            { "Elevación de gemelos sentado", "Gemelos", "Máquina" },
            { "Plancha", "Abdomen", "Peso corporal" },
            { "Crunch en polea", "Abdomen", "Polea" },
            { "Elevación de piernas colgado", "Abdomen", "Peso corporal" },
            { "Rueda abdominal", "Abdomen", "Otro" },
            { "Cinta de correr", "Cardio", "Máquina" },
            { "Bicicleta estática", "Cardio", "Máquina" },
            { "Elíptica", "Cardio", "Máquina" },
            { "Remo ergómetro", "Cardio", "Máquina" },
        };
        int creados = 0;
        for (String[] datos : biblioteca) {
            if (!existentes.add(normalizar(datos[0]))) {
                continue;
            }
            creados++;
            Ejercicio ejercicio = new Ejercicio();
            ejercicio.setNombre(datos[0]);
            ejercicio.setGrupoMuscular(datos[1]);
            ejercicio.setEquipamiento(datos[2]);
            ejercicioRepository.save(ejercicio);
        }
        if (creados > 0) {
            log.info("Biblioteca común: {} ejercicios nuevos", creados);
        }
    }

    private static String normalizar(String nombre) {
        return nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
    }
}
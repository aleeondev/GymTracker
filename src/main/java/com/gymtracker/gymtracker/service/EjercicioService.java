package com.gymtracker.gymtracker.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gymtracker.gymtracker.model.Ejercicio;
import com.gymtracker.gymtracker.repository.EjercicioRepository;
import com.gymtracker.gymtracker.repository.RutinaEjercicioRepository;
import com.gymtracker.gymtracker.repository.SerieEntrenamientoRepository;

@Service 
public class EjercicioService {

    public static final List<String> GRUPOS_MUSCULARES = List.of(
        "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Antebrazo",
        "Cuádriceps", "Femoral", "Glúteos", "Gemelos", "Abdomen", "Cardio");

    private final EjercicioRepository ejercicioRepository;
    private final RutinaEjercicioRepository rutinaEjercicioRepository;
    private final SerieEntrenamientoRepository serieRepository;

    public EjercicioService(EjercicioRepository ejercicioRepository,
                            RutinaEjercicioRepository rutinaEjercicioRepository,
                            SerieEntrenamientoRepository serieRepository) {
        this.ejercicioRepository = ejercicioRepository;
        this.rutinaEjercicioRepository = rutinaEjercicioRepository;
        this.serieRepository = serieRepository;
    }

    /** Biblioteca común + ejercicios propios del usuario. */
    public List<Ejercicio> obtenerVisibles(UUID usuarioId) {
        return ejercicioRepository.findVisibles(usuarioId);
    }

    /** Un ejercicio que el usuario puede usar (común o suyo). */
    public Ejercicio obtenerVisible(Long id, UUID usuarioId) {
        return ejercicioRepository.findById(id)
            .filter(e -> e.esComun() || usuarioId.equals(e.getUsuarioId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Un ejercicio que el usuario puede editar o borrar (solo los suyos). */
    public Ejercicio obtenerPropio(Long id, UUID usuarioId) {
        return ejercicioRepository.findById(id)
            .filter(e -> usuarioId.equals(e.getUsuarioId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Ejercicio guardar(Ejercicio datos, UUID usuarioId) {
        Ejercicio ejercicio = datos.getId() != null ? obtenerPropio(datos.getId(), usuarioId) : new Ejercicio();
        ejercicio.setUsuarioId(usuarioId);
        ejercicio.setNombre(limpiar(datos.getNombre()));
        ejercicio.setGrupoMuscular(limpiar(datos.getGrupoMuscular()));
        ejercicio.setEquipamiento(limpiar(datos.getEquipamiento()));
        ejercicio.setDescripcion(limpiar(datos.getDescripcion()));
        return ejercicioRepository.save(ejercicio);
    }

    /** Devuelve un mensaje de error si no se puede borrar, o null si se ha borrado. */
    @Transactional
    public String eliminar(Long id, UUID usuarioId) {
        Ejercicio ejercicio = obtenerPropio(id, usuarioId);
        if (serieRepository.existsByEjercicio(ejercicio)) {
            return "No se puede eliminar \"" + ejercicio.getNombre() + "\": ya aparece en tu historial de entrenamientos.";
        }
        if (rutinaEjercicioRepository.existsByEjercicio(ejercicio)) {
            return "No se puede eliminar \"" + ejercicio.getNombre() + "\": primero quítalo de las rutinas que lo usan.";
        }
        ejercicioRepository.delete(ejercicio);
        return null;
    }

    static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}

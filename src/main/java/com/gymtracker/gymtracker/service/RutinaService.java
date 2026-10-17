package com.gymtracker.gymtracker.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gymtracker.gymtracker.model.Rutina;
import com.gymtracker.gymtracker.model.RutinaEjercicio;
import com.gymtracker.gymtracker.repository.EntrenamientoRepository;
import com.gymtracker.gymtracker.repository.RutinaRepository;

@Service 
public class RutinaService {
    private final RutinaRepository rutinaRepository;
    private final EntrenamientoRepository entrenamientoRepository;
    private final EjercicioService ejercicioService;

    public RutinaService(RutinaRepository rutinaRepository, EntrenamientoRepository entrenamientoRepository,
                         EjercicioService ejercicioService) {
        this.rutinaRepository = rutinaRepository;
        this.entrenamientoRepository = entrenamientoRepository;
        this.ejercicioService = ejercicioService;
    }

    public List<Rutina> obtenerDelUsuario(UUID usuarioId) {
        return rutinaRepository.findByUsuarioIdOrderByNombreAsc(usuarioId);
    }

    public Rutina obtener(Long id, UUID usuarioId) {
        return rutinaRepository.findByIdAndUsuarioId(id, usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Rutina guardar(Rutina datos, UUID usuarioId) {
        Rutina rutina = datos.getId() != null ? obtener(datos.getId(), usuarioId) : new Rutina();
        rutina.setUsuarioId(usuarioId);
        rutina.setNombre(EjercicioService.limpiar(datos.getNombre()));
        rutina.setDescripcion(EjercicioService.limpiar(datos.getDescripcion()));
        return rutinaRepository.save(rutina);
    }

    @Transactional
    public void eliminar(Long id, UUID usuarioId) {
        Rutina rutina = obtener(id, usuarioId);
        // El historial se conserva: los entrenamientos de esta rutina se quedan sin rutina
        // (guardan su propio nombre, así que se siguen viendo igual).
        entrenamientoRepository.findByRutina(rutina).forEach(e -> e.setRutina(null));
        rutinaRepository.delete(rutina);
    }

    // ---------- Ejercicios dentro de la rutina ----------

    public RutinaEjercicio obtenerEjercicio(Long rutinaId, Long rutinaEjercicioId, UUID usuarioId) {
        return obtener(rutinaId, usuarioId).getEjercicios().stream()
            .filter(re -> re.getId().equals(rutinaEjercicioId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Crea o actualiza un ejercicio de la rutina. */
    @Transactional
    public void guardarEjercicio(Long rutinaId, RutinaEjercicio datos, Long ejercicioId, UUID usuarioId) {
        Rutina rutina = obtener(rutinaId, usuarioId);
        RutinaEjercicio re;
        if (datos.getId() != null) {
            re = obtenerEjercicio(rutinaId, datos.getId(), usuarioId);
        } else {
            re = new RutinaEjercicio();
            re.setRutina(rutina);
            re.setOrden(rutina.getEjercicios().size() + 1);
            rutina.getEjercicios().add(re);
        }
        re.setEjercicio(ejercicioService.obtenerVisible(ejercicioId, usuarioId));
        re.setSeries(entre(datos.getSeries(), 1, 20, 3));
        re.setRepeticiones(entre(datos.getRepeticiones(), 1, 100, 10));
        Double peso = datos.getPesoObjetivo();
        re.setPesoObjetivo(peso != null && peso >= 0 && peso <= 1500 ? peso : null);
        re.setDescanso(entre(datos.getDescanso(), 0, 900, 90));
        rutinaRepository.save(rutina);
    }

    @Transactional
    public void eliminarEjercicio(Long rutinaId, Long rutinaEjercicioId, UUID usuarioId) {
        Rutina rutina = obtener(rutinaId, usuarioId);
        rutina.getEjercicios().removeIf(re -> re.getId().equals(rutinaEjercicioId));
        renumerar(rutina.getEjercicios());
    }

    /** Sube (direccion = -1) o baja (direccion = 1) un ejercicio dentro de la rutina. */
    @Transactional
    public void moverEjercicio(Long rutinaId, Long rutinaEjercicioId, int direccion, UUID usuarioId) {
        List<RutinaEjercicio> lista = obtener(rutinaId, usuarioId).getEjercicios();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equals(rutinaEjercicioId)) {
                int destino = i + direccion;
                if (destino >= 0 && destino < lista.size()) {
                    lista.add(destino, lista.remove(i));
                    renumerar(lista);
                }
                return;
            }
        }
    }

    /** Valor dentro de [min, max]; si falta o se sale, el valor por defecto. */
    private static Integer entre(Integer valor, int min, int max, int porDefecto) {
        return valor != null && valor >= min && valor <= max ? valor : porDefecto;
    }

    private static void renumerar(List<RutinaEjercicio> lista) {
        for (int i = 0; i < lista.size(); i++) {
            lista.get(i).setOrden(i + 1);
        }
    }
}

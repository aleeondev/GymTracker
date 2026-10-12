package com.gymtracker.gymtracker.service;
import java.util.List;
import org.springframework.stereotype.Service;
import com.gymtracker.gymtracker.model.Rutina;
import com.gymtracker.gymtracker.model.RutinaEjercicio;
import com.gymtracker.gymtracker.repository.RutinaEjercicioRepository;

@Service 
public class RutinaEjercicioService {
    private final RutinaEjercicioRepository rutinaEjercicioRepository;

    public RutinaEjercicioService(RutinaEjercicioRepository rutinaEjercicioRepository) {
        this.rutinaEjercicioRepository = rutinaEjercicioRepository;
    }

    public List<RutinaEjercicio> obtenerTodos() {
        return rutinaEjercicioRepository.findAll();
    }

    public RutinaEjercicio guardar(RutinaEjercicio rutinaEjercicio) {
        return rutinaEjercicioRepository.save(rutinaEjercicio);
    }

    public void eliminar(Long id) {
        rutinaEjercicioRepository.deleteById(id);
    }

    public List<RutinaEjercicio> obtenerPorRutina(Rutina rutina) {
        return rutinaEjercicioRepository.findByRutinaOrderByOrdenAsc(rutina);
    }
}
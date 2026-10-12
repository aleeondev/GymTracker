package com.gymtracker.gymtracker.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gymtracker.gymtracker.model.Rutina;
import com.gymtracker.gymtracker.repository.RutinaRepository;

@Service 
public class RutinaService {
    private final RutinaRepository rutinaRepository;

    public RutinaService(RutinaRepository rutinaRepository) {
        this.rutinaRepository = rutinaRepository;
    }

    public List<Rutina> obtenerTodas() {
        return rutinaRepository.findAll();
    }

    public Rutina guardar(Rutina rutina) {
        return rutinaRepository.save(rutina);
    }

    public Optional<Rutina> obtenerPorId(Long id) {
        return rutinaRepository.findById(id);
    }

    public void eliminar(Long id) {
        rutinaRepository.deleteById(id);
    }
}

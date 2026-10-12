package com.gymtracker.gymtracker.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gymtracker.gymtracker.model.Ejercicio;
import com.gymtracker.gymtracker.repository.EjercicioRepository;

@Service 
public class EjercicioService {
    private final EjercicioRepository ejercicioRepository;

    public EjercicioService(EjercicioRepository ejercicioRepository) {
        this.ejercicioRepository = ejercicioRepository;
    }

    public List<Ejercicio> obtenerTodos() {
        return ejercicioRepository.findAll();
    }

    public Ejercicio guardar(Ejercicio ejercicio) {
        return ejercicioRepository.save(ejercicio);
    }

    public Optional<Ejercicio> obtenerPorId(Long id) {
        return ejercicioRepository.findById(id);
    }

    public void eliminar(Long id) {
        ejercicioRepository.deleteById(id);
    }

    public List<Ejercicio> obtenerPorGrupoMuscular(String grupoMuscular) {
        return ejercicioRepository.findByGrupoMuscular(grupoMuscular);
    }
}

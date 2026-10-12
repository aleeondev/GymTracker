package com.gymtracker.gymtracker.repository;

import com.gymtracker.gymtracker.model.Ejercicio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {
    List<Ejercicio> findByGrupoMuscular(String grupoMuscular);
}
package com.gymtracker.gymtracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymtracker.gymtracker.model.Rutina;
import com.gymtracker.gymtracker.model.RutinaEjercicio;

public interface RutinaEjercicioRepository extends JpaRepository<RutinaEjercicio, Long> {
    List<RutinaEjercicio> findByRutinaOrderByOrdenAsc(Rutina rutina);
}

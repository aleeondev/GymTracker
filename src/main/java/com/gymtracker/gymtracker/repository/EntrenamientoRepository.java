package com.gymtracker.gymtracker.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymtracker.gymtracker.model.Entrenamiento;
import com.gymtracker.gymtracker.model.Rutina;

public interface EntrenamientoRepository extends JpaRepository<Entrenamiento, Long> {

    List<Entrenamiento> findByUsuarioIdOrderByFechaDescCreadoEnDesc(UUID usuarioId);

    List<Entrenamiento> findByUsuarioIdAndFechaGreaterThanEqualOrderByFechaDesc(UUID usuarioId, LocalDate desde);

    Optional<Entrenamiento> findByIdAndUsuarioId(Long id, UUID usuarioId);

    long countByUsuarioId(UUID usuarioId);

    List<Entrenamiento> findByRutina(Rutina rutina);
}

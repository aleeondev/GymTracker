package com.gymtracker.gymtracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymtracker.gymtracker.model.Rutina;

public interface RutinaRepository extends JpaRepository<Rutina, Long> {

    List<Rutina> findByUsuarioIdOrderByNombreAsc(UUID usuarioId);

    Optional<Rutina> findByIdAndUsuarioId(Long id, UUID usuarioId);
}

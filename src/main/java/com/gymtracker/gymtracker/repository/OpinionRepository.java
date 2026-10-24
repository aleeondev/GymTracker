package com.gymtracker.gymtracker.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymtracker.gymtracker.model.Opinion;

public interface OpinionRepository extends JpaRepository<Opinion, Long> {

    /** Cuántas ha enviado el usuario desde una fecha (límite diario contra el spam). */
    long countByUsuarioIdAndCreadoEnAfter(UUID usuarioId, LocalDateTime desde);
}

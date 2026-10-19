package com.gymtracker.gymtracker.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymtracker.gymtracker.model.PesoCorporal;

public interface PesoCorporalRepository extends JpaRepository<PesoCorporal, Long> {

    /** Todos los registros del usuario, del más antiguo al más reciente (para la gráfica). */
    List<PesoCorporal> findByUsuarioIdOrderByFechaAsc(UUID usuarioId);

    Optional<PesoCorporal> findByUsuarioIdAndFecha(UUID usuarioId, LocalDate fecha);

    /** El más reciente: es el "peso actual". */
    Optional<PesoCorporal> findFirstByUsuarioIdOrderByFechaDesc(UUID usuarioId);
}

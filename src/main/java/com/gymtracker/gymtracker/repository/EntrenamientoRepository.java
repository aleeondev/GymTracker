package com.gymtracker.gymtracker.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gymtracker.gymtracker.model.Entrenamiento;
import com.gymtracker.gymtracker.model.Rutina;

public interface EntrenamientoRepository extends JpaRepository<Entrenamiento, Long> {

    List<Entrenamiento> findByUsuarioIdOrderByFechaDescCreadoEnDesc(UUID usuarioId);

    List<Entrenamiento> findByUsuarioIdAndFechaGreaterThanEqualOrderByFechaDesc(UUID usuarioId, LocalDate desde);

    Optional<Entrenamiento> findByIdAndUsuarioId(Long id, UUID usuarioId);

    long countByUsuarioId(UUID usuarioId);

    /** Al borrar una rutina, su historial se conserva pero sin enlace a ella. */
    @Modifying
    @Query("update Entrenamiento e set e.rutina = null where e.rutina = :rutina")
    void desvincularRutina(@Param("rutina") Rutina rutina);
}

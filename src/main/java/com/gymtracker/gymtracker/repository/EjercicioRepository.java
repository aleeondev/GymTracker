package com.gymtracker.gymtracker.repository;

import com.gymtracker.gymtracker.model.Ejercicio;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {

    /** Ejercicios comunes + los propios del usuario. */
    @Query("select e from Ejercicio e where e.usuarioId is null or e.usuarioId = :usuarioId order by e.grupoMuscular, e.nombre")
    List<Ejercicio> findVisibles(@Param("usuarioId") UUID usuarioId);

    List<Ejercicio> findByUsuarioIdIsNull();
}

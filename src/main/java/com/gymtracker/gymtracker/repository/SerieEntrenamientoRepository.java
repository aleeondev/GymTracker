package com.gymtracker.gymtracker.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gymtracker.gymtracker.model.Ejercicio;
import com.gymtracker.gymtracker.model.SerieEntrenamiento;

public interface SerieEntrenamientoRepository extends JpaRepository<SerieEntrenamiento, Long> {

    boolean existsByEjercicio(Ejercicio ejercicio);

    /** Todas las series del usuario, de la más reciente a la más antigua. */
    @Query("""
        select s from SerieEntrenamiento s
        join fetch s.entrenamiento e
        join fetch s.ejercicio
        where e.usuarioId = :usuarioId
        order by e.fecha desc, e.creadoEn desc, s.orden asc, s.numeroSerie asc
        """)
    List<SerieEntrenamiento> findDelUsuario(@Param("usuarioId") UUID usuarioId);
}

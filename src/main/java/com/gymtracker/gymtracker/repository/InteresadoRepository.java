package com.gymtracker.gymtracker.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymtracker.gymtracker.model.Interesado;

public interface InteresadoRepository extends JpaRepository<Interesado, Long> {

    boolean existsByEmail(String email);

    Optional<Interesado> findByTokenBaja(UUID tokenBaja);
}

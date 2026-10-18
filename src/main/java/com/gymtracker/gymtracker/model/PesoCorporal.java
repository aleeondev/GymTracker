package com.gymtracker.gymtracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Un registro de peso corporal: cuánto pesabas un día concreto.
 * Como mucho uno por usuario y día (si registras dos veces, se corrige el del día).
 * El último registrado es también el "peso actual" del perfil.
 */
@Entity
@Table(name = "pesos_corporales",
       uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "fecha" }))
@Getter
@Setter
public class PesoCorporal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now();
    }
}

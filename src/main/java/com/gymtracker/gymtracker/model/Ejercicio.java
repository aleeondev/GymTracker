package com.gymtracker.gymtracker.model;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Ejercicio de la biblioteca. Si usuarioId es null es un ejercicio común
 * (visible para todos, solo lectura); si no, es un ejercicio propio del usuario.
 */
@Entity 
@Table(name = "ejercicios")
@Getter
@Setter
public class Ejercicio {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    private String nombre;
    private String grupoMuscular;

    @Column(length = 1000)
    private String descripcion;

    private String equipamiento;

    public boolean esComun() {
        return usuarioId == null;
    }
}

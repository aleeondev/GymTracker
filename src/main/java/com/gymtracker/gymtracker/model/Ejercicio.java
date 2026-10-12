package com.gymtracker.gymtracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table(name = "ejercicios")
@Getter
@Setter
public class Ejercicio {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String grupoMuscular;

    @Column(length = 1000)
    private String descripcion;

    private String equipamiento;
}

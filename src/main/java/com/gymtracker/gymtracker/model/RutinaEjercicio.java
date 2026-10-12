package com.gymtracker.gymtracker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table(name = "rutina_ejercicios")
@Getter 
@Setter 
public class RutinaEjercicio {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @ManyToOne 
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;

    @ManyToOne 
    @JoinColumn(name = "ejercicio_id")
    private Ejercicio ejercicio;

    private Integer orden;
    private Integer series;
    private Integer repeticiones;
    private Double pesoObjetivo;
    private Integer descanso;
}
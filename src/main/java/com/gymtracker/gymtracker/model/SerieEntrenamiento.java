package com.gymtracker.gymtracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Una serie realizada dentro de un entrenamiento. */
@Entity
@Table(name = "series_entrenamiento")
@Getter
@Setter
public class SerieEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrenamiento_id", nullable = false)
    private Entrenamiento entrenamiento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    private Ejercicio ejercicio;

    /** Posición del ejercicio dentro del entrenamiento (1, 2, 3...). */
    @Column(nullable = false)
    private Integer orden;

    /** Número de serie dentro de ese ejercicio (1, 2, 3...). */
    @Column(nullable = false)
    private Integer numeroSerie;

    @Column(nullable = false)
    private Integer repeticiones;

    /** Kg. 0 para ejercicios con peso corporal. */
    @Column(nullable = false)
    private Double peso;

    public double getVolumen() {
        return peso * repeticiones;
    }

    /** 1RM estimado con la fórmula de Epley. */
    public double getUnoRmEstimado() {
        return repeticiones <= 1 ? peso : peso * (1 + repeticiones / 30.0);
    }
}

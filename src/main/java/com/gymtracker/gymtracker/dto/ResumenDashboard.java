package com.gymtracker.gymtracker.dto;

import java.util.List;

import com.gymtracker.gymtracker.model.Entrenamiento;

public record ResumenDashboard(
    long entrenosSemana,
    long entrenosMes,
    double volumenSemana,
    int rachaSemanas,
    long totalEntrenos,
    List<Entrenamiento> ultimos) {
}

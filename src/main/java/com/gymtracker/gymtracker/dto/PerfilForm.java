package com.gymtracker.gymtracker.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.gymtracker.gymtracker.model.Perfil;

import lombok.Getter;
import lombok.Setter;

/**
 * Lo que envía el cuestionario de bienvenida (/bienvenida).
 * Es una clase aparte de Perfil a propósito: así el formulario solo puede
 * tocar estos campos y nunca, por ejemplo, el usuarioId.
 */
@Getter
@Setter
public class PerfilForm {

    // Paso 1: sobre ti
    private String sexo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;
    private Integer alturaCm;
    private Double pesoKg;

    // Paso 2: tu objetivo
    private String objetivo;
    private Double pesoObjetivoKg;
    private String experiencia;

    // Paso 3: tu rutina
    private Integer diasSemana;
    private Integer duracionSesion;
    private String lugarEntreno;
    private String actividadDiaria;
    private String lesiones;

    /** Casilla de consentimiento (solo se pide si aún no lo dio al registrarse). */
    private boolean aceptoDatos;

    /** Rellena el formulario con lo que ya hay guardado (para editar). */
    public static PerfilForm desde(Perfil p) {
        PerfilForm f = new PerfilForm();
        f.sexo = p.getSexo();
        f.fechaNacimiento = p.getFechaNacimiento();
        f.alturaCm = p.getAlturaCm();
        f.pesoKg = p.getPesoKg();
        f.objetivo = p.getObjetivo();
        f.pesoObjetivoKg = p.getPesoObjetivoKg();
        f.experiencia = p.getExperiencia();
        f.diasSemana = p.getDiasSemana();
        f.duracionSesion = p.getDuracionSesion();
        f.lugarEntreno = p.getLugarEntreno();
        f.actividadDiaria = p.getActividadDiaria();
        f.lesiones = p.getLesiones();
        return f;
    }
}

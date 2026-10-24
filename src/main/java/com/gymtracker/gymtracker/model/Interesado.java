package com.gymtracker.gymtracker.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Persona apuntada a la lista de acceso anticipado (página /pronto).
 * Solo se guarda lo imprescindible: email, nombre opcional, cuándo dio su
 * consentimiento y un token secreto para darse de baja desde cualquier correo.
 */
@Entity
@Table(name = "interesados")
@Getter
@Setter
public class Interesado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Siempre en minúsculas y sin espacios (ver InteresadoService). */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(length = 40)
    private String nombre;

    /** Puntos del Fitness Challenge, si jugó antes de apuntarse. */
    private Integer puntuacion;

    /** Momento exacto en que aceptó recibir la invitación y novedades. */
    @Column(name = "consentimiento_en", nullable = false)
    private LocalDateTime consentimientoEn;

    /** Va en el enlace "darme de baja" de los correos. Secreto y único. */
    @Column(name = "token_baja", nullable = false, unique = true)
    private UUID tokenBaja;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now();
        if (tokenBaja == null) {
            tokenBaja = UUID.randomUUID();
        }
    }
}

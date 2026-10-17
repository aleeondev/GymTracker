package com.gymtracker.gymtracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos propios de cada usuario que guardamos en NUESTRA base de datos
 * (tabla "perfiles"), aparte de los de Supabase Auth (email, contraseña, nombre).
 *
 * Hay como mucho un perfil por usuario: su clave es el propio id de Supabase.
 * Se crea la primera vez que el usuario guarda algo en su perfil.
 *
 * Los datos del cuestionario de bienvenida (/bienvenida) también viven aquí.
 * Para añadir un campo nuevo: ponlo aquí (Hibernate crea la columna al
 * arrancar, por ddl-auto=update), en PerfilForm y en PerfilService.
 * Las opciones posibles de cada pregunta están en dto/OpcionesPerfil.
 */
@Entity
@Table(name = "perfiles")
@Getter
@Setter
public class Perfil {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    /** "@usuario" público y único: minúsculas, números y "_", de 3 a 20 caracteres. */
    @Column(name = "nombre_usuario", length = 20, unique = true)
    private String nombreUsuario;

    // ---------- Cuestionario de bienvenida ----------
    // Son datos de salud: solo los ve su dueño (la tabla tiene RLS activado)

    /** hombre / mujer / no_decir */
    @Column(length = 20)
    private String sexo;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "altura_cm")
    private Integer alturaCm;

    @Column(name = "peso_kg")
    private Double pesoKg;

    /** perder_grasa / definir / ganar_musculo / recomposicion / fuerza / salud */
    @Column(length = 30)
    private String objetivo;

    @Column(name = "peso_objetivo_kg")
    private Double pesoObjetivoKg;

    /** empiezo / menos_1 / de_1_a_3 / mas_3 */
    @Column(length = 20)
    private String experiencia;

    @Column(name = "dias_semana")
    private Integer diasSemana;

    /** Minutos que suele durar una sesión: 30 / 45 / 60 / 90 (opcional) */
    @Column(name = "duracion_sesion")
    private Integer duracionSesion;

    /** gimnasio / casa_material / casa_sin_material */
    @Column(name = "lugar_entreno", length = 30)
    private String lugarEntreno;

    /** Actividad fuera del gimnasio: sedentario / ligero / activo / muy_activo (opcional) */
    @Column(name = "actividad_diaria", length = 20)
    private String actividadDiaria;

    @Column(length = 500)
    private String lesiones;

    /** Cuándo aceptó que guardemos sus datos (en el registro o en el cuestionario). */
    @Column(name = "consentimiento_en")
    private LocalDateTime consentimientoEn;

    /** Cuándo completó o se saltó el cuestionario. Null = aún no lo ha visto. */
    @Column(name = "bienvenida_en")
    private LocalDateTime bienvenidaEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    // ---------- Cálculos (no se guardan: se calculan al pedirlos) ----------

    /** ¿Ha rellenado el cuestionario (al menos los datos obligatorios)? */
    public boolean isCuestionarioCompleto() {
        return pesoKg != null && alturaCm != null && objetivo != null;
    }

    public Integer getEdad() {
        return fechaNacimiento == null ? null : Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    /** Índice de masa corporal: peso / altura² (en metros). */
    public Double getImc() {
        if (pesoKg == null || alturaCm == null || alturaCm == 0) {
            return null;
        }
        double metros = alturaCm / 100.0;
        return Math.round(pesoKg / (metros * metros) * 10) / 10.0;
    }

    /** Categoría del IMC según la OMS. */
    public String getImcCategoria() {
        Double imc = getImc();
        if (imc == null) {
            return null;
        }
        if (imc < 18.5) {
            return "Bajo peso";
        }
        if (imc < 25) {
            return "Peso normal";
        }
        if (imc < 30) {
            return "Sobrepeso";
        }
        return "Obesidad";
    }

    /** Kg que faltan hasta el peso objetivo (positivo = subir, negativo = bajar). */
    public Double getDiferenciaObjetivo() {
        if (pesoKg == null || pesoObjetivoKg == null) {
            return null;
        }
        return Math.round((pesoObjetivoKg - pesoKg) * 10) / 10.0;
    }

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = LocalDateTime.now();
    }
}

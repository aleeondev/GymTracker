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
 * Una opinión enviada desde el botón "Opinar" de la app (versión alpha).
 * Se leen desde Supabase (Table Editor o SQL): la app no las enseña a nadie.
 */
@Entity
@Table(name = "opiniones")
@Getter
@Setter
public class Opinion {

    /** Tipos posibles (lo que se guarda en la columna "tipo"). */
    public static final String GUSTA = "gusta";
    public static final String MEJORA = "mejora";
    public static final String FALLO = "fallo";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /** gusta · mejora · fallo */
    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, length = 1000)
    private String texto;

    /** Página desde la que se envió (ej. /entrenamiento/nuevo), para saber de qué habla. */
    @Column(length = 200)
    private String pagina;

    /** Navegador y sistema (User-Agent), útil cuando "algo falla" solo en un móvil. */
    @Column(length = 250)
    private String navegador;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now();
    }
}

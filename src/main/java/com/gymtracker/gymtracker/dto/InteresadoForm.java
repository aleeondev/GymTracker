package com.gymtracker.gymtracker.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Formulario "Reservar mi plaza" de la página /pronto. */
@Getter
@Setter
public class InteresadoForm {

    @NotBlank(message = "Escribe tu email.")
    @Email(message = "Ese email no parece válido.")
    @Size(max = 254, message = "Ese email es demasiado largo.")
    private String email;

    @Size(max = 40, message = "El nombre puede tener como mucho 40 caracteres.")
    private String nombre;

    /** Puntos del Fitness Challenge (opcional). Máximo posible: 10 preguntas x 200. */
    @Min(value = 0, message = "Puntuación no válida.")
    @Max(value = 2000, message = "Puntuación no válida.")
    private Integer puntuacion;

    @AssertTrue(message = "Necesitamos tu permiso para escribirte.")
    private boolean consentimiento;

    /**
     * Trampa para bots: campo invisible para las personas. Si llega relleno,
     * respondemos como si todo hubiera ido bien pero no guardamos nada.
     */
    private String web;

    /** Sin espacios alrededor (al copiar y pegar en el móvil se cuelan) antes de validar. */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}

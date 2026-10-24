package com.gymtracker.gymtracker.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.dto.InteresadoForm;
import com.gymtracker.gymtracker.model.Interesado;
import com.gymtracker.gymtracker.repository.InteresadoRepository;

/** Lista de acceso anticipado: apuntarse y darse de baja. */
@Service
public class InteresadoService {

    public enum Resultado { APUNTADO, YA_ESTABA, IGNORADO }

    private final InteresadoRepository repository;

    public InteresadoService(InteresadoRepository repository) {
        this.repository = repository;
    }

    /**
     * Apunta a alguien a la lista. Sin @Transactional a propósito: si dos
     * peticiones con el mismo email llegan a la vez, la segunda choca con la
     * restricción "unique" y la tratamos como "ya estaba", sin error 500.
     */
    public Resultado apuntar(InteresadoForm form) {
        if (form.getWeb() != null && !form.getWeb().isBlank()) {
            return Resultado.IGNORADO; // bot
        }
        String email = normalizarEmail(form.getEmail());
        if (repository.existsByEmail(email)) {
            return Resultado.YA_ESTABA;
        }
        Interesado interesado = new Interesado();
        interesado.setEmail(email);
        interesado.setNombre(limpiarNombre(form.getNombre()));
        interesado.setPuntuacion(form.getPuntuacion());
        interesado.setConsentimientoEn(LocalDateTime.now());
        try {
            repository.save(interesado);
            return Resultado.APUNTADO;
        } catch (DataIntegrityViolationException e) {
            return Resultado.YA_ESTABA;
        }
    }

    public Optional<Interesado> buscarPorToken(String token) {
        return leerToken(token).flatMap(repository::findByTokenBaja);
    }

    /** Borra a la persona de la lista (sus datos desaparecen, no se "marcan"). */
    @Transactional
    public void darDeBaja(String token) {
        buscarPorToken(token).ifPresent(repository::delete);
    }

    static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String limpiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        return nombre.trim().replaceAll("\s+", " ");
    }

    private static Optional<UUID> leerToken(String token) {
        try {
            return token == null ? Optional.empty() : Optional.of(UUID.fromString(token.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

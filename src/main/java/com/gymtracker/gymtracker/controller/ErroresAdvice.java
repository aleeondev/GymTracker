package com.gymtracker.gymtracker.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Errores que queremos mostrar con una página concreta.
 * Si la base de datos no responde (Supabase caído, límite de conexiones...),
 * en vez de un 500 genérico mostramos la página 503 "Sin conexión con los datos".
 * El detalle técnico va al registro del servidor, nunca a la pantalla.
 */
@ControllerAdvice
public class ErroresAdvice {

    private static final Logger log = LoggerFactory.getLogger(ErroresAdvice.class);

    @ExceptionHandler({ CannotCreateTransactionException.class, DataAccessResourceFailureException.class })
    public ModelAndView baseDeDatosNoDisponible(Exception e) {
        log.error("Base de datos no disponible: {}", e.getMessage());
        ModelAndView pagina = new ModelAndView("error/503");
        pagina.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
        return pagina;
    }
}

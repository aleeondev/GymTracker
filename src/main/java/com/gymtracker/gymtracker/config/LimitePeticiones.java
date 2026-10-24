package com.gymtracker.gymtracker.config;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Límite sencillo de peticiones por IP para formularios públicos (lista de
 * espera de /pronto). Evita que alguien meta miles de emails con un script.
 * Vive en memoria: se reinicia al reiniciar la app, suficiente para este uso.
 *
 * Al publicar detrás de un proxy (Render) hay que activar
 * server.forward-headers-strategy=framework para ver la IP real del visitante.
 */
@Component
public class LimitePeticiones {

    static final int MAXIMO = 5;
    static final Duration VENTANA = Duration.ofMinutes(10);
    private static final int MAX_IPS_EN_MEMORIA = 10_000;

    private final Map<String, Deque<Instant>> intentos = new ConcurrentHashMap<>();

    /** true si esta IP aún puede hacer otra petición ahora. */
    public boolean permitir(String ip) {
        if (intentos.size() > MAX_IPS_EN_MEMORIA) {
            intentos.clear();
        }
        Instant ahora = Instant.now();
        Deque<Instant> lista = intentos.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (lista) {
            while (!lista.isEmpty() && lista.peekFirst().isBefore(ahora.minus(VENTANA))) {
                lista.pollFirst();
            }
            if (lista.size() >= MAXIMO) {
                return false;
            }
            lista.addLast(ahora);
            return true;
        }
    }
}

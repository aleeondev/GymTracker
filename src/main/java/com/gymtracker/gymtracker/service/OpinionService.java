package com.gymtracker.gymtracker.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.model.Opinion;
import com.gymtracker.gymtracker.repository.OpinionRepository;

/** Guarda las opiniones de los testers, validando todo en el servidor. */
@Service
public class OpinionService {

    /** Máximo de opiniones por persona y día (evita que alguien llene la tabla). */
    static final int MAXIMO_DIARIO = 15;
    static final int TEXTO_MAXIMO = 1000;

    private static final Set<String> TIPOS = Set.of(Opinion.GUSTA, Opinion.MEJORA, Opinion.FALLO);

    private final OpinionRepository opinionRepository;

    public OpinionService(OpinionRepository opinionRepository) {
        this.opinionRepository = opinionRepository;
    }

    /**
     * Guarda una opinión. Devuelve un mensaje de error o null si todo fue bien.
     * "pagina" ya llega validada (ruta interna o null) desde el controlador.
     */
    @Transactional
    public String enviar(UUID usuarioId, String tipo, String texto, String pagina, String navegador) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            return "Elige si es algo que te gusta, que mejorarías o que falla.";
        }
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.length() < 3) {
            return "Cuéntanos un poco más (al menos 3 caracteres).";
        }
        if (limpio.length() > TEXTO_MAXIMO) {
            return "El texto es demasiado largo (máximo " + TEXTO_MAXIMO + " caracteres).";
        }
        long hoy = opinionRepository.countByUsuarioIdAndCreadoEnAfter(
            usuarioId, EntrenamientoService.hoy().atStartOfDay());
        if (hoy >= MAXIMO_DIARIO) {
            return "Hoy ya nos has mandado muchas opiniones. ¡Gracias! Vuelve a probar mañana.";
        }

        Opinion opinion = new Opinion();
        opinion.setUsuarioId(usuarioId);
        opinion.setTipo(tipo);
        opinion.setTexto(limpio);
        opinion.setPagina(recortar(pagina, 200));
        opinion.setNavegador(recortar(navegador, 250));
        opinionRepository.save(opinion);
        return null;
    }

    public boolean haOpinado(UUID usuarioId) {
        return opinionRepository.existsByUsuarioId(usuarioId);
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.length() > maximo ? valor.substring(0, maximo) : valor;
    }
}

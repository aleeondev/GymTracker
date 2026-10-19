package com.gymtracker.gymtracker.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.model.Perfil;
import com.gymtracker.gymtracker.model.PesoCorporal;
import com.gymtracker.gymtracker.repository.PerfilRepository;
import com.gymtracker.gymtracker.repository.PesoCorporalRepository;

/** Registro del peso corporal y su relación con el "peso actual" del perfil. */
@Service
public class PesoCorporalService {

    private final PesoCorporalRepository pesoRepository;
    private final PerfilRepository perfilRepository;

    public PesoCorporalService(PesoCorporalRepository pesoRepository, PerfilRepository perfilRepository) {
        this.pesoRepository = pesoRepository;
        this.perfilRepository = perfilRepository;
    }

    public List<PesoCorporal> historial(UUID usuarioId) {
        return pesoRepository.findByUsuarioIdOrderByFechaAsc(usuarioId);
    }

    /** El registro de hoy, si ya lo hay (para el panel: "Hoy: 76,5 kg"). */
    public Optional<PesoCorporal> deHoy(UUID usuarioId) {
        return pesoRepository.findByUsuarioIdAndFecha(usuarioId, EntrenamientoService.hoy());
    }

    /**
     * Guarda el peso de un día (si ya había uno ese día, lo corrige).
     * Devuelve un mensaje de error o null si todo fue bien.
     * Si es el registro más reciente, también pasa a ser el peso actual del perfil.
     */
    @Transactional
    public String registrar(UUID usuarioId, Double pesoKg, LocalDate fecha) {
        LocalDate hoy = EntrenamientoService.hoy();
        LocalDate dia = fecha != null ? fecha : hoy;
        if (pesoKg == null || pesoKg.isNaN() || pesoKg < 30 || pesoKg > 300) {
            return "El peso debe estar entre 30 y 300 kg.";
        }
        if (dia.isAfter(hoy) || dia.isBefore(hoy.minusYears(1))) {
            return "La fecha debe estar entre hoy y hace un año.";
        }

        double redondeado = Math.round(pesoKg * 10) / 10.0;
        PesoCorporal registro = pesoRepository.findByUsuarioIdAndFecha(usuarioId, dia).orElseGet(() -> {
            PesoCorporal nuevo = new PesoCorporal();
            nuevo.setUsuarioId(usuarioId);
            nuevo.setFecha(dia);
            return nuevo;
        });
        registro.setPesoKg(redondeado);
        pesoRepository.save(registro);

        // ¿Es el más reciente? Entonces es tu peso actual.
        PesoCorporal ultimo = pesoRepository.findFirstByUsuarioIdOrderByFechaDesc(usuarioId).orElse(registro);
        if (!ultimo.getFecha().isAfter(dia)) {
            perfilRepository.findById(usuarioId).ifPresent(perfil -> actualizarPerfil(perfil, redondeado));
        }
        return null;
    }

    private void actualizarPerfil(Perfil perfil, double pesoKg) {
        perfil.setPesoKg(pesoKg);
        perfilRepository.save(perfil);
    }
}

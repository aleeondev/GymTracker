package com.gymtracker.gymtracker.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymtracker.gymtracker.dto.OpcionesPerfil;
import com.gymtracker.gymtracker.dto.PerfilForm;
import com.gymtracker.gymtracker.model.Perfil;
import com.gymtracker.gymtracker.repository.PerfilRepository;

@Service
public class PerfilService {

    /** Formato del nombre de usuario: 3-20 caracteres, minúsculas, números y "_". */
    public static final Pattern FORMATO_USUARIO = Pattern.compile("^[a-z0-9_]{3,20}$");

    private final PerfilRepository perfilRepository;
    private final PesoCorporalService pesoCorporalService;

    public PerfilService(PerfilRepository perfilRepository, PesoCorporalService pesoCorporalService) {
        this.perfilRepository = perfilRepository;
        this.pesoCorporalService = pesoCorporalService;
    }

    /** El perfil del usuario o, si aún no tiene, uno vacío (sin guardar). */
    public Perfil obtener(UUID usuarioId) {
        return perfilRepository.findById(usuarioId).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsuarioId(usuarioId);
            return nuevo;
        });
    }

    /**
     * Guarda el nombre de usuario. Devuelve un mensaje de error para el usuario
     * o null si todo fue bien. Vacío = quitarlo.
     */
    @Transactional
    public String guardarNombreUsuario(UUID usuarioId, String texto) {
        String nombre = normalizarUsuario(texto);
        if (nombre != null && !FORMATO_USUARIO.matcher(nombre).matches()) {
            return "El nombre de usuario debe tener entre 3 y 20 caracteres: solo letras minúsculas, números y \"_\".";
        }
        if (nombre != null && perfilRepository.existsByNombreUsuarioAndUsuarioIdNot(nombre, usuarioId)) {
            return "El nombre de usuario @" + nombre + " ya está cogido. Prueba con otro.";
        }
        Perfil perfil = obtener(usuarioId);
        perfil.setNombreUsuario(nombre);
        perfilRepository.save(perfil);
        return null;
    }

    /**
     * Devuelve el perfil del usuario y, si es su primera vez, lo crea con lo que
     * eligió al registrarse (guardado en Supabase como user_metadata):
     * el nombre de usuario (si sigue libre) y la fecha del consentimiento.
     */
    @Transactional
    public Perfil asegurarPerfil(UUID usuarioId, String usuarioRegistro, boolean consintioEnRegistro) {
        return perfilRepository.findById(usuarioId).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsuarioId(usuarioId);
            String nombre = normalizarUsuario(usuarioRegistro);
            if (nombre != null && FORMATO_USUARIO.matcher(nombre).matches() && !perfilRepository.existsByNombreUsuario(nombre)) {
                nuevo.setNombreUsuario(nombre);
            }
            if (consintioEnRegistro) {
                nuevo.setConsentimientoEn(LocalDateTime.now());
            }
            return perfilRepository.save(nuevo);
        });
    }

    /** Para el registro: "formato" (no válido), "cogido" o "libre". */
    public String disponibilidad(String texto) {
        String nombre = normalizarUsuario(texto);
        if (nombre == null || !FORMATO_USUARIO.matcher(nombre).matches()) {
            return "formato";
        }
        return perfilRepository.existsByNombreUsuario(nombre) ? "cogido" : "libre";
    }

    /** "Saltar por ahora": no volveremos a mandarle al cuestionario. */
    @Transactional
    public void saltarBienvenida(UUID usuarioId) {
        Perfil perfil = obtener(usuarioId);
        if (perfil.getBienvenidaEn() == null) {
            perfil.setBienvenidaEn(LocalDateTime.now());
        }
        perfilRepository.save(perfil);
    }

    /**
     * Guarda el cuestionario. Devuelve un mensaje de error o null si todo fue bien.
     * El navegador ya valida, pero aquí se vuelve a comprobar todo: nunca nos
     * fiamos de lo que llega de fuera.
     */
    @Transactional
    public String guardarCuestionario(UUID usuarioId, PerfilForm f) {
        Perfil perfil = obtener(usuarioId);

        if (!OpcionesPerfil.SEXO.containsKey(f.getSexo())) {
            return "Elige una opción en «Sexo».";
        }
        LocalDate hoy = LocalDate.now();
        if (f.getFechaNacimiento() == null || f.getFechaNacimiento().isAfter(hoy.minusYears(12))
                || f.getFechaNacimiento().isBefore(hoy.minusYears(100))) {
            return "Revisa tu fecha de nacimiento (debes tener entre 12 y 100 años).";
        }
        if (f.getAlturaCm() == null || f.getAlturaCm() < 100 || f.getAlturaCm() > 250) {
            return "La altura debe estar entre 100 y 250 cm.";
        }
        if (!entre(f.getPesoKg(), 30, 300)) {
            return "El peso debe estar entre 30 y 300 kg.";
        }
        if (!OpcionesPerfil.OBJETIVO.containsKey(f.getObjetivo())) {
            return "Elige tu objetivo principal.";
        }
        Double pesoObjetivo = f.getPesoObjetivoKg(); // opcional, para cualquier objetivo
        if (pesoObjetivo != null && !entre(pesoObjetivo, 30, 300)) {
            return "El peso objetivo debe estar entre 30 y 300 kg.";
        }
        if (!OpcionesPerfil.EXPERIENCIA.containsKey(f.getExperiencia())) {
            return "Elige tu experiencia en el gimnasio.";
        }
        if (f.getDiasSemana() == null || !OpcionesPerfil.DIAS.containsKey(String.valueOf(f.getDiasSemana()))) {
            return "Elige cuántos días por semana puedes entrenar.";
        }
        if (!OpcionesPerfil.LUGAR.containsKey(f.getLugarEntreno())) {
            return "Elige dónde entrenas.";
        }
        if (f.getDuracionSesion() != null && !OpcionesPerfil.DURACION.containsKey(String.valueOf(f.getDuracionSesion()))) {
            return "Duración de la sesión no válida.";
        }
        String actividad = vacioANull(f.getActividadDiaria());
        if (actividad != null && !OpcionesPerfil.ACTIVIDAD.containsKey(actividad)) {
            return "Actividad diaria no válida.";
        }
        if (perfil.getConsentimientoEn() == null && !f.isAceptoDatos()) {
            return "Para guardar estos datos necesitamos que aceptes cómo los usamos (casilla del final).";
        }

        perfil.setSexo(f.getSexo());
        perfil.setFechaNacimiento(f.getFechaNacimiento());
        perfil.setAlturaCm(f.getAlturaCm());
        perfil.setPesoKg(Math.round(f.getPesoKg() * 10) / 10.0);
        perfil.setObjetivo(f.getObjetivo());
        perfil.setPesoObjetivoKg(pesoObjetivo != null ? Math.round(pesoObjetivo * 10) / 10.0 : null);
        perfil.setExperiencia(f.getExperiencia());
        perfil.setDiasSemana(f.getDiasSemana());
        perfil.setDuracionSesion(f.getDuracionSesion());
        perfil.setLugarEntreno(f.getLugarEntreno());
        perfil.setActividadDiaria(actividad);
        String lesiones = EjercicioService.limpiar(f.getLesiones());
        perfil.setLesiones(lesiones != null && lesiones.length() > 500 ? lesiones.substring(0, 500) : lesiones);
        if (perfil.getConsentimientoEn() == null) {
            perfil.setConsentimientoEn(LocalDateTime.now());
        }
        if (perfil.getBienvenidaEn() == null) {
            perfil.setBienvenidaEn(LocalDateTime.now());
        }
        perfilRepository.save(perfil);
        // El peso del cuestionario cuenta como el registro de hoy (primer punto de la gráfica)
        pesoCorporalService.registrar(usuarioId, perfil.getPesoKg(), null);
        return null;
    }

    /** Texto para mostrar de una opción guardada: etiqueta(OpcionesPerfil.LUGAR, "gimnasio") -> "Gimnasio completo" */
    public static String etiqueta(Map<String, String> opciones, Object clave) {
        return clave == null ? null : opciones.get(String.valueOf(clave));
    }

    private static boolean entre(Double valor, double min, double max) {
        return valor != null && !valor.isNaN() && valor >= min && valor <= max;
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }

    /** " @Alex_Gym " -> "alex_gym"; vacío -> null */
    public static String normalizarUsuario(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.startsWith("@")) {
            limpio = limpio.substring(1);
        }
        limpio = limpio.toLowerCase(Locale.ROOT);
        return limpio.isEmpty() ? null : limpio;
    }
}

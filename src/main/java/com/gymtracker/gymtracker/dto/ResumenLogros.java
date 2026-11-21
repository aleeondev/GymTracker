package com.gymtracker.gymtracker.dto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Todos los logros de un usuario, listos para la página "Mis logros". */
public record ResumenLogros(List<Logro> logros) {

    public long getConseguidos() {
        return logros.stream().filter(Logro::isConseguido).count();
    }

    public int getTotal() {
        return logros.size();
    }

    /** Los logros agrupados por categoría, en el orden en que se definieron. */
    public Map<String, List<Logro>> getPorCategoria() {
        Map<String, List<Logro>> mapa = new LinkedHashMap<>();
        logros.forEach(l -> mapa.computeIfAbsent(l.categoria(), k -> new ArrayList<>()).add(l));
        return mapa;
    }

    /** La medalla conseguida más reciente (con fecha conocida), o null. */
    public Logro getUltimo() {
        return logros.stream()
            .filter(Logro::isConseguido)
            .filter(l -> l.conseguidoEn() != null)
            .max(Comparator.comparing(Logro::conseguidoEn))
            .orElse(null);
    }

    /** El logro sin conseguir al que menos le falta (en %), para animar: "¡Te falta poco!". */
    public Logro getMasCercano() {
        return logros.stream()
            .filter(l -> !l.isConseguido())
            .max(Comparator.comparingDouble(l -> (double) l.actual() / l.objetivo()))
            .filter(l -> l.actual() > 0)
            .orElse(null);
    }

    public boolean isAlgunoConseguido() {
        return logros.stream().anyMatch(Logro::isConseguido);
    }

}

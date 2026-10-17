package com.gymtracker.gymtracker.config;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.gymtracker.gymtracker.service.EntrenamientoService;

/** Formatos para las plantillas: se usa como ${@fmt.kg(peso)}. */
@Component("fmt")
public class Formato {

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter CORTA = DateTimeFormatter.ofPattern("EEE d MMM", ES);
    private static final DateTimeFormatter LARGA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM yyyy", ES);

    /** 60.0 -> "60", 62.5 -> "62,5" */
    public String num(Double valor) {
        if (valor == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(ES));
        return df.format(valor);
    }

    public String kg(Double valor) {
        return valor == null ? "-" : num(valor) + " kg";
    }

    /** Números grandes sin decimales: 12450.5 -> "12.451" */
    public String entero(Double valor) {
        if (valor == null) {
            return "0";
        }
        DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(ES));
        df.setGroupingSize(3);
        df.setGroupingUsed(true);
        return df.format(Math.round(valor));
    }

    public String fecha(LocalDate fecha) {
        return capitalizar(CORTA.format(fecha).replace(".", ""));
    }

    public String fechaLarga(LocalDate fecha) {
        return capitalizar(LARGA.format(fecha));
    }

    public String mes(LocalDate fecha) {
        return capitalizar(MES.format(fecha));
    }

    /** "Hoy", "Ayer", "Hace 3 días" o la fecha corta. */
    public String relativa(LocalDate fecha) {
        long dias = ChronoUnit.DAYS.between(fecha, EntrenamientoService.hoy());
        if (dias == 0) {
            return "Hoy";
        }
        if (dias == 1) {
            return "Ayer";
        }
        if (dias > 1 && dias < 7) {
            return "Hace " + dias + " días";
        }
        return fecha(fecha);
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
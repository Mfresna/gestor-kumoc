package org.kumoc.ui;

import org.kumoc.exception.NegocioException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos de presentacion (es-AR). */
public final class Formato {

    private static final Locale AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() {
    }

    public static String dinero(BigDecimal valor) {

        NumberFormat formato = NumberFormat.getCurrencyInstance(AR);
        return formato.format(valor);
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    public static String fechaHora(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : fechaHora.format(FECHA_HORA);
    }

    public static String siNo(boolean valor) {
        return valor ? "Sí" : "No";
    }

    /** Convierte texto como "1.250,50" o "1250.50" a BigDecimal; null si esta vacio. */
    public static BigDecimal aDinero(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim().replace("$", "").replace(" ", "");
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(',', '.');
        }
        return new BigDecimal(limpio);
    }

    /** Lee un entero obligatorio de un campo de texto; el nombre del campo se usa en el mensaje de error. */
    public static int aEntero(String texto, String campo) {

        try {
            return Integer.parseInt(texto == null ? "" : texto.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException(campo + " debe ser un número entero.");
        }
    }

    /** Lee un monto de un campo de texto; null si esta vacio y lanza un error de negocio si no es numerico. */
    public static BigDecimal aDineroOError(String texto, String campo) {

        try {
            return aDinero(texto);
        } catch (NumberFormatException e) {
            throw new NegocioException(campo + " no es un número válido.");
        }
    }
}

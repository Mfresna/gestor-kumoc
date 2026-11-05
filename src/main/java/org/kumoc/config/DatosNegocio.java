package org.kumoc.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Datos del negocio para el pie de los PDF, leidos de %APPDATA%\Kumoc\negocio.properties (UTF-8).
 * Los campos vacios no se imprimen.
 */
public record DatosNegocio(String banco, String titular, String cuenta,
                           String email, String telefono, String direccion) {

    private static final String PLANTILLA = """
            # Datos del negocio que se imprimen en el pie de los presupuestos en PDF.
            # Completá los que quieras mostrar; los vacíos se omiten. Guardá el archivo en UTF-8.
            pago.banco=
            pago.titular=
            pago.cuenta=
            contacto.email=
            contacto.telefono=
            contacto.direccion=
            """;

    /** Lee el archivo; si no existe crea una plantilla vacia. Ante un error de lectura devuelve datos vacios. */
    public static DatosNegocio cargar() {

        Path ruta = AppPaths.getBusinessDataPath();
        try {
            if (Files.notExists(ruta)) {
                try (Writer escritor = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
                    escritor.write(PLANTILLA);
                }
            }
            Properties props = new Properties();
            try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
                props.load(lector);
            }
            return new DatosNegocio(
                    valor(props, "pago.banco"), valor(props, "pago.titular"), valor(props, "pago.cuenta"),
                    valor(props, "contacto.email"), valor(props, "contacto.telefono"),
                    valor(props, "contacto.direccion"));
        } catch (IOException e) {
            return new DatosNegocio("", "", "", "", "", "");
        }
    }

    /** Lineas de "Informacion para el pago" (solo las completas). */
    public List<String> lineasPago() {

        List<String> lineas = new ArrayList<>();
        agregar(lineas, banco);
        agregar(lineas, titular.isEmpty() ? "" : "Beneficiario: " + titular);
        agregar(lineas, cuenta.isEmpty() ? "" : "Cuenta: " + cuenta);
        return lineas;
    }

    /** Lineas de "Datos de contacto" (solo las completas). */
    public List<String> lineasContacto() {

        List<String> lineas = new ArrayList<>();
        agregar(lineas, email);
        agregar(lineas, telefono);
        agregar(lineas, direccion);
        return lineas;
    }

    private static void agregar(List<String> lineas, String valor) {
        if (!valor.isEmpty()) {
            lineas.add(valor);
        }
    }

    private static String valor(Properties props, String clave) {
        return props.getProperty(clave, "").trim();
    }
}

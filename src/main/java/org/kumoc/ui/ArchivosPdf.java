package org.kumoc.ui;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;

/** Dialogo para elegir donde guardar un PDF. */
public final class ArchivosPdf {

    private ArchivosPdf() {
    }

    /** Devuelve el destino elegido (con extension .pdf) o null si el usuario cancelo. */
    public static Path elegirDestino(Window propietaria, String nombreSugerido) {

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar PDF");
        selector.setInitialFileName(nombreSugerido);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        File documentos = new File(System.getProperty("user.home"), "Documents");
        if (documentos.isDirectory()) {
            selector.setInitialDirectory(documentos);
        }

        File archivo = selector.showSaveDialog(propietaria);
        if (archivo == null) {
            return null;
        }
        Path ruta = archivo.toPath();
        return ruta.getFileName().toString().toLowerCase().endsWith(".pdf")
                ? ruta
                : ruta.resolveSibling(ruta.getFileName() + ".pdf");
    }
}

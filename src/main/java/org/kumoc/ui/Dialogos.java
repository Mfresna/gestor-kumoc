package org.kumoc.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import org.kumoc.exception.AccesoDatosException;
import org.kumoc.exception.NegocioException;

/** Mensajes al usuario. Nunca muestra detalles tecnicos: los errores inesperados solo van al log. */
public final class Dialogos {

    private static final String CSS = Dialogos.class.getResource("/css/kumoc.css").toExternalForm();

    private Dialogos() {
    }

    /** Ejecuta una accion de interfaz mostrando un dialogo si falla. */
    public static void ejecutar(Runnable accion) {

        try {
            accion.run();
        } catch (RuntimeException e) {
            error(e);
        }
    }

    public static void error(Throwable e) {

        if (e instanceof NegocioException) {
            mostrar(Alert.AlertType.WARNING, "No se pudo completar la acción", e.getMessage());
            return;
        }

        e.printStackTrace();
        String mensaje = e instanceof AccesoDatosException
                ? e.getMessage()
                : "Ocurrió un error inesperado.";
        mostrar(Alert.AlertType.ERROR, "Error", mensaje);
    }

    public static void info(String titulo, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, titulo, mensaje);
    }

    public static boolean confirmar(String titulo, String mensaje) {

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.OK)).setText("Aceptar");
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        configurar(alerta, titulo);
        return alerta.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {

        Alert alerta = new Alert(tipo, mensaje, ButtonType.OK);
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.OK)).setText("Aceptar");
        configurar(alerta, titulo);
        alerta.showAndWait();
    }

    private static void configurar(Alert alerta, String titulo) {

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        DialogPane panel = alerta.getDialogPane();
        panel.getStylesheets().add(CSS);
        panel.getStyleClass().add("contenido");
    }
}

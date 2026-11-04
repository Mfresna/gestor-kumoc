package org.kumoc.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/** Apertura de ventanas modales a partir de un FXML. */
public final class Ventanas {

    private static final String CSS = Ventanas.class.getResource("/css/kumoc.css").toExternalForm();

    private Ventanas() {
    }

    /**
     * Carga el FXML, deja que el llamador prepare su controlador, muestra la ventana y espera a que
     * se cierre. Devuelve el controlador para leer el resultado.
     */
    public static <C> C mostrarModal(String vista, String titulo, Window propietaria, Consumer<C> preparar) {

        try {
            FXMLLoader loader = new FXMLLoader(Ventanas.class.getResource("/view/" + vista));
            Parent raiz = loader.load();
            C controlador = loader.getController();
            preparar.accept(controlador);

            Scene escena = new Scene(raiz);
            escena.getStylesheets().add(CSS);

            Stage ventana = new Stage();
            ventana.initOwner(propietaria);
            ventana.initModality(Modality.WINDOW_MODAL);
            ventana.setTitle(titulo);
            ventana.setScene(escena);
            ventana.setResizable(false);
            ventana.showAndWait();
            return controlador;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Cierra la ventana a la que pertenece el nodo dado. */
    public static void cerrar(Node nodo) {
        nodo.getScene().getWindow().hide();
    }
}

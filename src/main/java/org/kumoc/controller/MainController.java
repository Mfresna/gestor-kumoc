package org.kumoc.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.EstadoApp;

import java.io.IOException;
import java.util.List;

/** Ventana principal: menu lateral y area de contenido donde se carga cada seccion. */
public class MainController {

    @FXML
    private StackPane contenido;
    @FXML
    private Button btnProductos;
    @FXML
    private Button btnClientes;
    @FXML
    private Button btnPresupuestos;
    @FXML
    private Button btnOrdenes;
    @FXML
    private Button btnStock;
    @FXML
    private Label badgeStock;

    private Seccion seccionActual;

    @FXML
    private void initialize() {

        badgeStock.textProperty().bind(EstadoApp.alertasStock().asString());
        badgeStock.visibleProperty().bind(EstadoApp.alertasStock().greaterThan(0));
        Dialogos.ejecutar(EstadoApp::refrescarAlertas);

        mostrarProductos();
    }

    @FXML
    private void mostrarProductos() {
        mostrar("ProductosView.fxml", btnProductos);
    }

    @FXML
    private void mostrarClientes() {
        mostrar("ClientesView.fxml", btnClientes);
    }

    @FXML
    private void mostrarPresupuestos() {
        mostrar("PresupuestosView.fxml", btnPresupuestos);
    }

    @FXML
    private void mostrarOrdenes() {
        mostrar("OrdenesView.fxml", btnOrdenes);
    }

    @FXML
    private void mostrarStock() {
        mostrar("StockView.fxml", btnStock);
    }

    private void mostrar(String vista, Button activo) {

        if (seccionActual != null) {
            seccionActual.alSalir();
            seccionActual = null;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + vista));
            Node nodo = loader.load();
            contenido.getChildren().setAll(nodo);

            List.of(btnProductos, btnClientes, btnPresupuestos, btnOrdenes, btnStock)
                    .forEach(b -> b.getStyleClass().remove("activo"));
            activo.getStyleClass().add("activo");

            if (loader.getController() instanceof Seccion seccion) {
                seccionActual = seccion;
                Dialogos.ejecutar(seccion::alMostrar);
            }
        } catch (IOException e) {
            Dialogos.error(e);
        }
    }
}

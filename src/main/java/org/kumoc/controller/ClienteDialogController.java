package org.kumoc.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.kumoc.AppContext;
import org.kumoc.model.Cliente;
import org.kumoc.model.TipoFactura;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.Ventanas;

/** Alta rapida de un cliente desde otra ventana (por ejemplo, Nuevo presupuesto). */
public class ClienteDialogController {

    @FXML
    private TextField txtRazonSocial;
    @FXML
    private TextField txtCuit;
    @FXML
    private ComboBox<TipoFactura> cmbTipoFactura;
    @FXML
    private TextField txtTelefono;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtDireccion;
    @FXML
    private TextField txtLocalidad;
    @FXML
    private TextField txtCodigoPostal;
    @FXML
    private TextField txtProvincia;

    private Cliente guardado;

    @FXML
    private void initialize() {
        cmbTipoFactura.getItems().setAll(TipoFactura.values());
        cmbTipoFactura.setValue(TipoFactura.X);
    }

    /** Cliente creado, o null si el usuario cancelo. */
    public Cliente guardado() {
        return guardado;
    }

    @FXML
    private void guardar() {

        Dialogos.ejecutar(() -> {
            guardado = AppContext.get().clientes().guardar(new Cliente(
                    null, txtRazonSocial.getText(), txtCuit.getText(), cmbTipoFactura.getValue(),
                    txtEmail.getText(), txtTelefono.getText(), txtDireccion.getText(),
                    txtLocalidad.getText(), txtCodigoPostal.getText(), txtProvincia.getText(), true));
            Ventanas.cerrar(txtRazonSocial);
        });
    }

    @FXML
    private void cancelar() {
        guardado = null;
        Ventanas.cerrar(txtRazonSocial);
    }
}

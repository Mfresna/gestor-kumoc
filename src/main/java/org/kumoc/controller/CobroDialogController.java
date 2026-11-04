package org.kumoc.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import org.kumoc.exception.NegocioException;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.Ventanas;

import java.time.LocalDate;

/** Pide la fecha de cobro y si la orden queda marcada para facturar en ARCA. */
public class CobroDialogController {

    /** Datos confirmados por el usuario. */
    public record Resultado(LocalDate fechaCobro, boolean facturarArca) {
    }

    @FXML
    private Label lblDetalle;
    @FXML
    private DatePicker dpFecha;
    @FXML
    private CheckBox chkFacturar;

    private Resultado resultado;

    public void preparar(String detalle) {

        lblDetalle.setText(detalle);
        dpFecha.setValue(LocalDate.now());
        chkFacturar.setSelected(false);
    }

    /** Resultado confirmado, o null si el usuario cancelo. */
    public Resultado resultado() {
        return resultado;
    }

    @FXML
    private void aceptar() {

        Dialogos.ejecutar(() -> {
            if (dpFecha.getValue() == null) {
                throw new NegocioException("La fecha de cobro es obligatoria.");
            }
            resultado = new Resultado(dpFecha.getValue(), chkFacturar.isSelected());
            Ventanas.cerrar(dpFecha);
        });
    }

    @FXML
    private void cancelar() {
        resultado = null;
        Ventanas.cerrar(dpFecha);
    }
}

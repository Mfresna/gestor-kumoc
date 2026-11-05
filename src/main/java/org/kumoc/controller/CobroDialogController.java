package org.kumoc.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.kumoc.exception.NegocioException;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.Formato;
import org.kumoc.ui.Ventanas;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pide fecha y monto del cobro (sugiere el saldo completo, editable para un pago parcial) y,
 * si el pago salda la orden, si queda marcada para facturar en ARCA.
 */
public class CobroDialogController {

    /** Datos confirmados por el usuario. */
    public record Resultado(LocalDate fechaCobro, BigDecimal monto, boolean facturarArca) {
    }

    @FXML
    private Label lblDetalle;
    @FXML
    private Label lblResumen;
    @FXML
    private DatePicker dpFecha;
    @FXML
    private TextField txtMonto;
    @FXML
    private Label lblAviso;
    @FXML
    private CheckBox chkFacturar;
    @FXML
    private Label lblAyudaFacturar;

    private BigDecimal saldo = BigDecimal.ZERO;
    private Resultado resultado;

    @FXML
    private void initialize() {
        txtMonto.textProperty().addListener((obs, anterior, texto) -> actualizarAviso());
    }

    public void preparar(String detalle, BigDecimal total, BigDecimal pagado, BigDecimal saldo) {

        this.saldo = saldo;
        lblDetalle.setText(detalle);
        lblResumen.setText("Total " + Formato.dinero(total) + "  ·  Ya pagado " + Formato.dinero(pagado)
                + "  ·  Saldo " + Formato.dinero(saldo));
        dpFecha.setValue(LocalDate.now());
        txtMonto.setText(saldo.toPlainString().replace('.', ','));
        chkFacturar.setSelected(false);
        actualizarAviso();
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
            BigDecimal monto = leerMonto();
            if (monto == null || monto.signum() <= 0) {
                throw new NegocioException("El monto del pago debe ser mayor a cero.");
            }
            if (monto.compareTo(saldo) > 0) {
                throw new NegocioException("El monto supera el saldo pendiente (" + Formato.dinero(saldo) + ").");
            }
            boolean salda = monto.compareTo(saldo) == 0;
            resultado = new Resultado(dpFecha.getValue(), monto, salda && chkFacturar.isSelected());
            Ventanas.cerrar(dpFecha);
        });
    }

    @FXML
    private void cancelar() {
        resultado = null;
        Ventanas.cerrar(dpFecha);
    }

    private void actualizarAviso() {

        BigDecimal monto = montoSinError();
        boolean valido = monto != null && monto.signum() > 0 && monto.compareTo(saldo) <= 0;
        boolean salda = valido && monto.compareTo(saldo) == 0;

        if (monto == null || monto.signum() <= 0) {
            lblAviso.setText("Ingresá el monto que se paga.");
        } else if (monto.compareTo(saldo) > 0) {
            lblAviso.setText("El monto supera el saldo pendiente.");
        } else if (salda) {
            lblAviso.setText("Este pago salda la orden.");
        } else {
            lblAviso.setText("Pago parcial: quedará un saldo de " + Formato.dinero(saldo.subtract(monto)) + ".");
        }

        chkFacturar.setVisible(salda);
        chkFacturar.setManaged(salda);
        lblAyudaFacturar.setVisible(salda);
        lblAyudaFacturar.setManaged(salda);
    }

    private BigDecimal leerMonto() {
        return Formato.aDineroOError(txtMonto.getText(), "El monto");
    }

    /** Version tolerante para el aviso en vivo: texto incompleto o invalido se trata como "sin monto". */
    private BigDecimal montoSinError() {

        try {
            return leerMonto();
        } catch (RuntimeException e) {
            return null;
        }
    }
}

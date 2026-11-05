package org.kumoc.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.kumoc.exception.NegocioException;
import org.kumoc.model.Producto;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.Formato;
import org.kumoc.ui.Ventanas;

import java.util.List;

/** Dialogo reutilizable para ingreso, ajuste y fabricacion: pide producto, cantidad y (opcional) motivo. */
public class MovimientoDialogController {

    /** Datos ingresados por el usuario. */
    public record Resultado(int idProducto, int cantidad, String motivo) {
    }

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblAyuda;
    @FXML
    private VBox boxProducto;
    @FXML
    private ComboBox<Producto> cmbProducto;
    @FXML
    private Label lblCantidad;
    @FXML
    private TextField txtCantidad;
    @FXML
    private VBox boxMotivo;
    @FXML
    private TextField txtMotivo;

    private boolean pideMotivo;
    private String motivoFijo;
    private Resultado resultado;

    @FXML
    private void initialize() {

        cmbProducto.setCellFactory(lista -> celda());
        cmbProducto.setButtonCell(celda());
    }

    /**
     * Configura el dialogo.
     *
     * @param titulo         titulo mostrado
     * @param ayuda          texto de ayuda (puede ser null)
     * @param productos      productos elegibles
     * @param seleccionado   producto preseleccionado (puede ser null)
     * @param bloquearProducto si el usuario no puede cambiar el producto
     * @param etiquetaCantidad texto de la etiqueta de cantidad
     * @param cantidadInicial valor inicial de cantidad (puede ser null)
     * @param motivoFijo     si no es null, el motivo no se pide y se usa este valor
     */
    public void preparar(String titulo, String ayuda, List<Producto> productos, Producto seleccionado,
                         boolean bloquearProducto, String etiquetaCantidad, Integer cantidadInicial,
                         String motivoFijo) {

        lblTitulo.setText(titulo);
        lblAyuda.setText(ayuda == null ? "" : ayuda);
        lblAyuda.setVisible(ayuda != null);
        lblAyuda.setManaged(ayuda != null);

        cmbProducto.getItems().setAll(productos);
        cmbProducto.setValue(seleccionado);
        cmbProducto.setDisable(bloquearProducto);

        lblCantidad.setText(etiquetaCantidad);
        txtCantidad.setText(cantidadInicial == null ? "" : String.valueOf(cantidadInicial));

        this.motivoFijo = motivoFijo;
        this.pideMotivo = motivoFijo == null;
        boxMotivo.setVisible(pideMotivo);
        boxMotivo.setManaged(pideMotivo);
    }

    /** Resultado confirmado, o null si el usuario cancelo. */
    public Resultado resultado() {
        return resultado;
    }

    @FXML
    private void aceptar() {

        Dialogos.ejecutar(() -> {
            Producto producto = cmbProducto.getValue();
            if (producto == null) {
                throw new NegocioException("Elegí un producto.");
            }
            int cantidad = Formato.aEntero(txtCantidad.getText(), "La cantidad");
            String motivo = pideMotivo ? txtMotivo.getText() : motivoFijo;

            resultado = new Resultado(producto.idProducto(), cantidad, motivo);
            Ventanas.cerrar(txtCantidad);
        });
    }

    @FXML
    private void cancelar() {
        resultado = null;
        Ventanas.cerrar(txtCantidad);
    }

    private ListCell<Producto> celda() {

        return new ListCell<>() {
            @Override
            protected void updateItem(Producto p, boolean vacio) {
                super.updateItem(p, vacio);
                setText(vacio || p == null ? null : descripcion(p));
            }
        };
    }

    /** Texto con que se muestra un producto en listas desplegables. */
    public static String descripcion(Producto p) {

        StringBuilder texto = new StringBuilder();
        if (p.codigo() != null) {
            texto.append(p.codigo()).append(" – ");
        }
        texto.append(p.nombre());
        if (p.talle() != null) {
            texto.append(" (").append(p.talle()).append(")");
        }
        return texto.toString();
    }
}

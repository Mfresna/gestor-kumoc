package org.kumoc.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.kumoc.AppContext;
import org.kumoc.model.MovimientoStock;
import org.kumoc.model.Producto;
import org.kumoc.model.StockProducto;
import org.kumoc.model.TipoMovimiento;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.EstadoApp;
import org.kumoc.ui.Formato;
import org.kumoc.ui.Tablas;
import org.kumoc.ui.Ventanas;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Stock calculado por producto, alertas de stock bajo, ingresos/ajustes e historial de movimientos. */
public class StockController implements Seccion {

    /** Fila de la tabla: un producto activo con su stock calculado. */
    public record FilaStock(Producto producto, StockProducto stock) {
        boolean bajo() {
            return stock.fisico() < producto.stockMinimo();
        }
    }

    @FXML
    private TextField txtBuscar;
    @FXML
    private HBox bannerAlerta;
    @FXML
    private Label lblAlerta;
    @FXML
    private TableView<FilaStock> tabla;
    @FXML
    private TableColumn<FilaStock, String> colCodigo;
    @FXML
    private TableColumn<FilaStock, String> colProducto;
    @FXML
    private TableColumn<FilaStock, String> colFisico;
    @FXML
    private TableColumn<FilaStock, String> colComprometido;
    @FXML
    private TableColumn<FilaStock, String> colDisponible;
    @FXML
    private TableColumn<FilaStock, String> colMinimo;

    @FXML
    private Label lblHistorial;
    @FXML
    private Button btnIngreso;
    @FXML
    private Button btnAjuste;
    @FXML
    private TableView<MovimientoStock> tablaMovimientos;
    @FXML
    private TableColumn<MovimientoStock, String> colMovFecha;
    @FXML
    private TableColumn<MovimientoStock, String> colMovTipo;
    @FXML
    private TableColumn<MovimientoStock, String> colMovCantidad;
    @FXML
    private TableColumn<MovimientoStock, String> colMovMotivo;

    private final ObservableList<FilaStock> filas = FXCollections.observableArrayList();
    private FilteredList<FilaStock> filtradas;

    @FXML
    private void initialize() {

        colCodigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().producto().codigo()));
        colProducto.setCellValueFactory(c -> new ReadOnlyStringWrapper(nombreCompleto(c.getValue().producto())));
        colFisico.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().stock().fisico())));
        colComprometido.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().stock().comprometido())));
        colDisponible.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().stock().disponible())));
        colMinimo.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().producto().stockMinimo())));

        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(FilaStock fila, boolean vacio) {
                super.updateItem(fila, vacio);
                getStyleClass().remove("alerta");
                if (!vacio && fila != null && fila.bajo()) {
                    getStyleClass().add("alerta");
                }
            }
        });
        tabla.setPlaceholder(new Label("No hay productos activos."));

        filtradas = new FilteredList<>(filas, f -> true);
        tabla.setItems(filtradas);
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> filtrar(texto));

        colMovFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.fechaHora(c.getValue().fecha())));
        colMovTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(etiquetaTipo(c.getValue().tipo())));
        colMovCantidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(signo(c.getValue())));
        colMovMotivo.setCellValueFactory(c -> new ReadOnlyStringWrapper(motivo(c.getValue())));
        tablaMovimientos.setPlaceholder(new Label("Sin movimientos."));
        Tablas.ajustarColumnas(tabla, tablaMovimientos);

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, fila) -> mostrarHistorial(fila));
        mostrarHistorial(null);
    }

    @Override
    public void alMostrar() {
        recargar();
    }

    @FXML
    private void ingreso() {

        FilaStock fila = tabla.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return;
        }

        Dialogos.ejecutar(() -> {
            MovimientoDialogController dialogo = Ventanas.mostrarModal("MovimientoDialog.fxml", "Registrar ingreso",
                    tabla.getScene().getWindow(),
                    (MovimientoDialogController c) -> c.preparar("REGISTRAR INGRESO",
                            "Suma unidades al stock (compra, fabricación, devolución…).",
                            List.of(fila.producto()), fila.producto(), true, "CANTIDAD *", null, null));

            MovimientoDialogController.Resultado r = dialogo.resultado();
            if (r != null) {
                AppContext.get().stock().registrarIngreso(r.idProducto(), r.cantidad(), r.motivo(), null);
                despuesDeMovimiento(r.idProducto());
            }
        });
    }

    @FXML
    private void ajuste() {

        FilaStock fila = tabla.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return;
        }

        Dialogos.ejecutar(() -> {
            MovimientoDialogController dialogo = Ventanas.mostrarModal("MovimientoDialog.fxml", "Registrar ajuste",
                    tabla.getScene().getWindow(),
                    (MovimientoDialogController c) -> c.preparar("REGISTRAR AJUSTE",
                            "Corrige el stock. Usá un número positivo para sumar o negativo para restar (por ejemplo -2).",
                            List.of(fila.producto()), fila.producto(), true, "AJUSTE (+/−) *", null, null));

            MovimientoDialogController.Resultado r = dialogo.resultado();
            if (r != null) {
                AppContext.get().stock().registrarAjuste(r.idProducto(), r.cantidad(), r.motivo());
                despuesDeMovimiento(r.idProducto());
            }
        });
    }

    private void despuesDeMovimiento(int idProducto) {

        EstadoApp.refrescarAlertas();
        recargar();
        filas.stream()
                .filter(f -> f.producto().idProducto() == idProducto)
                .findFirst()
                .ifPresent(tabla.getSelectionModel()::select);
    }

    private void recargar() {

        Integer idSeleccionado = tabla.getSelectionModel().getSelectedItem() == null
                ? null : tabla.getSelectionModel().getSelectedItem().producto().idProducto();

        Map<Integer, StockProducto> stock = AppContext.get().stock().obtenerStockTodos();
        filas.setAll(AppContext.get().productos().listar(true).stream()
                .map(p -> new FilaStock(p, stock.getOrDefault(p.idProducto(), new StockProducto(p.idProducto(), 0, 0))))
                .toList());

        long bajos = filas.stream().filter(FilaStock::bajo).count();
        bannerAlerta.setVisible(bajos > 0);
        bannerAlerta.setManaged(bajos > 0);
        lblAlerta.setText(bajos == 1
                ? "1 producto está por debajo de su stock mínimo."
                : bajos + " productos están por debajo de su stock mínimo.");
        EstadoApp.refrescarAlertas();

        if (idSeleccionado != null) {
            filas.stream()
                    .filter(f -> f.producto().idProducto().equals(idSeleccionado))
                    .findFirst()
                    .ifPresent(tabla.getSelectionModel()::select);
        }
        mostrarHistorial(tabla.getSelectionModel().getSelectedItem());
    }

    private void mostrarHistorial(FilaStock fila) {

        boolean hay = fila != null;
        lblHistorial.setText(hay ? "MOVIMIENTOS – " + fila.producto().nombre().toUpperCase(Locale.ROOT) : "HISTORIAL DE MOVIMIENTOS");
        tablaMovimientos.getItems().setAll(hay
                ? AppContext.get().stock().historial(fila.producto().idProducto())
                : List.of());
        btnIngreso.setDisable(!hay);
        btnAjuste.setDisable(!hay);
    }

    private void filtrar(String texto) {

        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        filtradas.setPredicate(f -> filtro.isEmpty()
                || f.producto().codigo().toLowerCase(Locale.ROOT).contains(filtro)
                || nombreCompleto(f.producto()).toLowerCase(Locale.ROOT).contains(filtro)
                || (f.producto().color() != null && f.producto().color().toLowerCase(Locale.ROOT).contains(filtro)));
    }

    private static String nombreCompleto(Producto p) {
        return p.talle() == null ? p.nombre() : p.nombre() + " (" + p.talle() + ")";
    }

    private static String etiquetaTipo(TipoMovimiento tipo) {

        return switch (tipo) {
            case INGRESO -> "Ingreso";
            case EGRESO -> "Egreso";
            case AJUSTE -> "Ajuste";
        };
    }

    private static String signo(MovimientoStock m) {

        int efecto = m.efecto();
        return efecto > 0 ? "+" + efecto : String.valueOf(efecto);
    }

    private static String motivo(MovimientoStock m) {
        return m.idPresupuesto() == null ? m.motivo() : m.motivo() + " (N° " + m.idPresupuesto() + ")";
    }
}

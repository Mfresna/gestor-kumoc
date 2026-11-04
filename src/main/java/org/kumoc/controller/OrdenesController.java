package org.kumoc.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.kumoc.AppContext;
import org.kumoc.model.Cliente;
import org.kumoc.model.EstadoPresupuesto;
import org.kumoc.model.ItemPresupuesto;
import org.kumoc.model.Presupuesto;
import org.kumoc.model.Producto;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.EstadoApp;
import org.kumoc.ui.Formato;
import org.kumoc.ui.Tablas;
import org.kumoc.ui.Ventanas;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Ordenes de trabajo y ventas: fabricacion de faltantes, venta, cobro y facturacion en ARCA. */
public class OrdenesController implements Seccion {

    private static final String TODAS = "Órdenes y ventas";
    private static final String SOLO_ORDENES = "Solo órdenes de trabajo";
    private static final String SOLO_VENTAS = "Solo ventas";
    private static final String PENDIENTES_COBRO = "Pendientes de cobro";
    private static final String PENDIENTES_ARCA = "Pendientes de facturar en ARCA (fin de mes)";

    /** Fila del detalle: lo pedido contra el stock fisico y lo que falta fabricar. */
    private record LineaDetalle(Producto producto, int pedido, int fisico, int faltante) {
    }

    @FXML
    private ComboBox<String> cmbFiltro;
    @FXML
    private TableView<Presupuesto> tabla;
    @FXML
    private TableColumn<Presupuesto, String> colNumero;
    @FXML
    private TableColumn<Presupuesto, String> colFecha;
    @FXML
    private TableColumn<Presupuesto, String> colCliente;
    @FXML
    private TableColumn<Presupuesto, String> colTotal;
    @FXML
    private TableColumn<Presupuesto, String> colEstado;
    @FXML
    private TableColumn<Presupuesto, String> colCobro;
    @FXML
    private TableColumn<Presupuesto, String> colArca;

    @FXML
    private Label lblDetalle;
    @FXML
    private Label lblResumen;
    @FXML
    private TableView<LineaDetalle> tablaItems;
    @FXML
    private TableColumn<LineaDetalle, String> colItemProducto;
    @FXML
    private TableColumn<LineaDetalle, String> colItemPedido;
    @FXML
    private TableColumn<LineaDetalle, String> colItemFisico;
    @FXML
    private TableColumn<LineaDetalle, String> colItemFaltante;

    @FXML
    private Button btnFabricacion;
    @FXML
    private Button btnVenta;
    @FXML
    private Button btnCobro;
    @FXML
    private Button btnFacturada;

    private final ObservableList<Presupuesto> ordenes = FXCollections.observableArrayList();
    private final Map<Integer, Cliente> clientes = new HashMap<>();
    private final Map<Integer, Producto> productos = new HashMap<>();

    @FXML
    private void initialize() {

        colNumero.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().idPresupuesto())));
        colFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.fecha(c.getValue().fechaEmision())));
        colCliente.setCellValueFactory(c -> new ReadOnlyStringWrapper(nombreCliente(c.getValue().idCliente())));
        colTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().total())));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().estado() == EstadoPresupuesto.ORDEN_TRABAJO ? "En producción" : "Venta registrada"));
        colCobro.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().cobrado() ? Formato.fecha(c.getValue().fechaCobro()) : "Pendiente"));
        colArca.setCellValueFactory(c -> new ReadOnlyStringWrapper(textoArca(c.getValue())));
        tabla.setItems(ordenes);
        tabla.setPlaceholder(new Label("No hay órdenes para mostrar."));

        colItemProducto.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                MovimientoDialogController.descripcion(c.getValue().producto())));
        colItemPedido.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().pedido())));
        colItemFisico.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().fisico())));
        colItemFaltante.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().faltante())));
        tablaItems.setPlaceholder(new Label("Seleccioná una orden para ver su detalle."));
        Tablas.ajustarColumnas(tabla, tablaItems);

        cmbFiltro.getItems().setAll(TODAS, SOLO_ORDENES, SOLO_VENTAS, PENDIENTES_COBRO, PENDIENTES_ARCA);
        cmbFiltro.setValue(TODAS);
        cmbFiltro.valueProperty().addListener((obs, anterior, valor) -> Dialogos.ejecutar(this::recargar));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, p) -> mostrarDetalle(p));
        mostrarDetalle(null);
    }

    @Override
    public void alMostrar() {
        recargar();
    }

    @FXML
    private void registrarFabricacion() {

        accionSobreSeleccion(orden -> {
            Map<Integer, Integer> faltantes = AppContext.get().presupuestos().calcularFaltantes(orden.idPresupuesto());
            List<Producto> deLaOrden = orden.items().stream().map(i -> productos.get(i.idProducto())).toList();
            Producto sugerido = deLaOrden.stream()
                    .filter(p -> faltantes.containsKey(p.idProducto()))
                    .findFirst()
                    .orElse(null);

            MovimientoDialogController dialogo = Ventanas.mostrarModal("MovimientoDialog.fxml",
                    "Registrar fabricación", tabla.getScene().getWindow(),
                    (MovimientoDialogController c) -> c.preparar(
                            "REGISTRAR FABRICACIÓN",
                            "Las unidades fabricadas ingresan al stock con el motivo \"Fabricación\", vinculadas a la orden N° "
                                    + orden.idPresupuesto() + ".",
                            deLaOrden, sugerido, false, "UNIDADES FABRICADAS *",
                            sugerido == null ? null : faltantes.get(sugerido.idProducto()), "Fabricación"));

            MovimientoDialogController.Resultado r = dialogo.resultado();
            if (r != null) {
                AppContext.get().presupuestos().registrarFabricacion(orden.idPresupuesto(), r.idProducto(), r.cantidad());
                EstadoApp.refrescarAlertas();
                recargar();
            }
        });
    }

    @FXML
    private void pasarAVenta() {

        accionSobreSeleccion(orden -> {
            if (Dialogos.confirmar("Pasar a venta",
                    "Se registrará la venta de la orden N° " + orden.idPresupuesto()
                            + " y se descontará el stock de cada producto. ¿Continuar?")) {
                AppContext.get().presupuestos().pasarAVenta(orden.idPresupuesto());
                EstadoApp.refrescarAlertas();
                recargar();
            }
        });
    }

    @FXML
    private void registrarCobro() {

        accionSobreSeleccion(orden -> {
            CobroDialogController dialogo = Ventanas.mostrarModal("CobroDialog.fxml", "Registrar cobro",
                    tabla.getScene().getWindow(),
                    (CobroDialogController c) -> c.preparar("Orden N° " + orden.idPresupuesto() + " – "
                            + nombreCliente(orden.idCliente()) + " – " + Formato.dinero(orden.total())));

            CobroDialogController.Resultado r = dialogo.resultado();
            if (r != null) {
                AppContext.get().presupuestos().registrarCobro(orden.idPresupuesto(), r.fechaCobro(), r.facturarArca());
                recargar();
            }
        });
    }

    @FXML
    private void marcarFacturada() {

        accionSobreSeleccion(orden -> {
            if (Dialogos.confirmar("Facturada en ARCA",
                    "¿Marcar la orden N° " + orden.idPresupuesto() + " como facturada en ARCA con fecha de hoy?")) {
                AppContext.get().presupuestos().marcarFacturadoArca(orden.idPresupuesto(), LocalDate.now());
                recargar();
            }
        });
    }

    private void accionSobreSeleccion(Consumer<Presupuesto> accion) {

        Presupuesto orden = tabla.getSelectionModel().getSelectedItem();
        if (orden != null) {
            Dialogos.ejecutar(() -> accion.accept(orden));
        }
    }

    private void recargar() {

        clientes.clear();
        AppContext.get().clientes().listar().forEach(c -> clientes.put(c.idCliente(), c));
        productos.clear();
        AppContext.get().productos().listar(false).forEach(p -> productos.put(p.idProducto(), p));

        Integer idSeleccionado = tabla.getSelectionModel().getSelectedItem() == null
                ? null : tabla.getSelectionModel().getSelectedItem().idPresupuesto();

        List<Presupuesto> lista = PENDIENTES_ARCA.equals(cmbFiltro.getValue())
                ? AppContext.get().presupuestos().listarPendientesFacturarArca()
                : AppContext.get().presupuestos().listar(null).stream()
                        .filter(p -> p.estado() == EstadoPresupuesto.ORDEN_TRABAJO || p.estado() == EstadoPresupuesto.VENTA)
                        .filter(this::cumpleFiltro)
                        .toList();
        ordenes.setAll(lista);

        if (idSeleccionado != null) {
            ordenes.stream()
                    .filter(p -> p.idPresupuesto().equals(idSeleccionado))
                    .findFirst()
                    .ifPresent(tabla.getSelectionModel()::select);
        }
        mostrarDetalle(tabla.getSelectionModel().getSelectedItem());
    }

    private boolean cumpleFiltro(Presupuesto p) {

        String filtro = cmbFiltro.getValue();
        if (SOLO_ORDENES.equals(filtro)) {
            return p.estado() == EstadoPresupuesto.ORDEN_TRABAJO;
        }
        if (SOLO_VENTAS.equals(filtro)) {
            return p.estado() == EstadoPresupuesto.VENTA;
        }
        if (PENDIENTES_COBRO.equals(filtro)) {
            return !p.cobrado();
        }
        return true;
    }

    private void mostrarDetalle(Presupuesto orden) {

        boolean hay = orden != null;
        boolean enProduccion = hay && orden.estado() == EstadoPresupuesto.ORDEN_TRABAJO;
        Map<Integer, Integer> faltantes = enProduccion
                ? AppContext.get().presupuestos().calcularFaltantes(orden.idPresupuesto())
                : Map.of();

        lblDetalle.setText(hay ? "DETALLE DE LA ORDEN N° " + orden.idPresupuesto() : "DETALLE");
        lblResumen.setText(hay ? resumen(orden, faltantes) : "");
        tablaItems.getItems().setAll(hay ? lineas(orden, faltantes) : List.of());

        btnFabricacion.setDisable(!enProduccion);
        btnVenta.setDisable(!enProduccion || !faltantes.isEmpty());
        btnCobro.setDisable(!hay || orden.cobrado());
        btnFacturada.setDisable(!hay || !orden.cobrado() || !orden.facturarArca() || orden.fechaFacturacionArca() != null);
    }

    private List<LineaDetalle> lineas(Presupuesto orden, Map<Integer, Integer> faltantes) {

        return orden.items().stream()
                .map((ItemPresupuesto i) -> new LineaDetalle(
                        productos.get(i.idProducto()), i.cantidad(),
                        AppContext.get().stock().obtenerStock(i.idProducto()).fisico(),
                        faltantes.getOrDefault(i.idProducto(), 0)))
                .toList();
    }

    private String resumen(Presupuesto orden, Map<Integer, Integer> faltantes) {

        if (orden.estado() == EstadoPresupuesto.VENTA) {
            return "Venta registrada. " + (orden.cobrado() ? "Cobrada el " + Formato.fecha(orden.fechaCobro()) + "." : "Pendiente de cobro.");
        }
        return faltantes.isEmpty()
                ? "Stock completo: la orden está lista para pasar a venta."
                : "Faltan fabricar unidades de " + faltantes.size() + " producto(s) antes de registrar la venta.";
    }

    private static String textoArca(Presupuesto p) {

        if (!p.facturarArca()) {
            return p.cobrado() ? "No factura" : "—";
        }
        return p.fechaFacturacionArca() == null
                ? "Pendiente"
                : "Facturada " + Formato.fecha(p.fechaFacturacionArca());
    }

    private String nombreCliente(int idCliente) {
        Cliente c = clientes.get(idCliente);
        return c == null ? "(cliente eliminado)" : c.razonSocial();
    }
}

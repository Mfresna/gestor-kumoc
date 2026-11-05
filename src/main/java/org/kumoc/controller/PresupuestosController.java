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
import org.kumoc.config.AppPaths;
import org.kumoc.config.DatosNegocio;
import org.kumoc.exception.NegocioException;
import org.kumoc.model.Cliente;
import org.kumoc.model.EstadoPresupuesto;
import org.kumoc.model.ItemPresupuesto;
import org.kumoc.model.Presupuesto;
import org.kumoc.model.Producto;
import org.kumoc.pdf.PresupuestoPdf;
import org.kumoc.ui.ArchivosPdf;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.EstadoApp;
import org.kumoc.ui.Formato;
import org.kumoc.ui.Tablas;
import org.kumoc.ui.Ventanas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Presupuestos y rechazados. Al aprobarse, el presupuesto pasa a la seccion de ordenes de trabajo. */
public class PresupuestosController implements Seccion {

    private static final String TODOS = "Presupuestos y rechazados";
    private static final String SOLO_PRESUPUESTOS = "Solo presupuestos";
    private static final String SOLO_RECHAZADOS = "Solo rechazados";

    @FXML
    private ComboBox<String> cmbEstado;
    @FXML
    private TableView<Presupuesto> tabla;
    @FXML
    private TableColumn<Presupuesto, String> colNumero;
    @FXML
    private TableColumn<Presupuesto, String> colFecha;
    @FXML
    private TableColumn<Presupuesto, String> colCliente;
    @FXML
    private TableColumn<Presupuesto, String> colItems;
    @FXML
    private TableColumn<Presupuesto, String> colTotal;
    @FXML
    private TableColumn<Presupuesto, String> colEstado;

    @FXML
    private Label lblDetalle;
    @FXML
    private TableView<ItemPresupuesto> tablaItems;
    @FXML
    private TableColumn<ItemPresupuesto, String> colItemProducto;
    @FXML
    private TableColumn<ItemPresupuesto, String> colItemCantidad;
    @FXML
    private TableColumn<ItemPresupuesto, String> colItemPrecio;
    @FXML
    private TableColumn<ItemPresupuesto, String> colItemSubtotal;

    @FXML
    private Button btnEditar;
    @FXML
    private Button btnAprobar;
    @FXML
    private Button btnRechazar;
    @FXML
    private Button btnEliminar;
    @FXML
    private Button btnImprimir;

    private final ObservableList<Presupuesto> presupuestos = FXCollections.observableArrayList();
    private final Map<Integer, Cliente> clientes = new HashMap<>();
    private final Map<Integer, Producto> productos = new HashMap<>();

    @FXML
    private void initialize() {

        colNumero.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().idPresupuesto())));
        colFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.fecha(c.getValue().fechaEmision())));
        colCliente.setCellValueFactory(c -> new ReadOnlyStringWrapper(nombreCliente(c.getValue().idCliente())));
        colItems.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().items().size())));
        colTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().total())));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(etiquetaEstado(c.getValue().estado())));
        tabla.setItems(presupuestos);
        tabla.setPlaceholder(new Label("No hay presupuestos para mostrar."));

        colItemProducto.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                MovimientoDialogController.descripcion(productos.get(c.getValue().idProducto()))));
        colItemCantidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().cantidad())));
        colItemPrecio.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().precioUnitario())));
        colItemSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().subtotal())));
        tablaItems.setPlaceholder(new Label("Seleccioná un presupuesto para ver su detalle."));
        Tablas.ajustarColumnas(tabla, tablaItems);

        cmbEstado.getItems().setAll(TODOS, SOLO_PRESUPUESTOS, SOLO_RECHAZADOS);
        cmbEstado.setValue(TODOS);
        cmbEstado.valueProperty().addListener((obs, anterior, valor) -> Dialogos.ejecutar(this::recargar));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, p) -> mostrarDetalle(p));
        mostrarDetalle(null);
    }

    @Override
    public void alMostrar() {
        recargar();
    }

    @FXML
    private void nuevo() {
        abrirEditor(null);
    }

    @FXML
    private void editar() {

        Presupuesto p = tabla.getSelectionModel().getSelectedItem();
        if (p != null) {
            abrirEditor(p);
        }
    }

    @FXML
    private void aprobar() {

        accionSobreSeleccion(p -> {
            if (Dialogos.confirmar("Aprobar presupuesto",
                    "El presupuesto N° " + p.idPresupuesto() + " pasará a orden de trabajo y su stock quedará "
                            + "comprometido (congelado). ¿Continuar?")) {
                AppContext.get().presupuestos().aprobarComoOrden(p.idPresupuesto());
                Dialogos.info("Orden creada", "Lo encontrás en la sección Órdenes de trabajo.");
                EstadoApp.refrescarAlertas();
                recargar();
            }
        });
    }

    @FXML
    private void rechazar() {

        accionSobreSeleccion(p -> {
            if (Dialogos.confirmar("Rechazar presupuesto", "¿Marcar el presupuesto N° " + p.idPresupuesto() + " como rechazado?")) {
                AppContext.get().presupuestos().rechazar(p.idPresupuesto());
                recargar();
            }
        });
    }

    @FXML
    private void eliminar() {

        accionSobreSeleccion(p -> {
            if (Dialogos.confirmar("Eliminar presupuesto",
                    "¿Eliminar el presupuesto N° " + p.idPresupuesto() + "? Esta acción no se puede deshacer.")) {
                AppContext.get().presupuestos().eliminar(p.idPresupuesto());
                recargar();
            }
        });
    }

    @FXML
    private void imprimirPdf() {

        accionSobreSeleccion(p -> {
            Cliente cliente = clientes.get(p.idCliente());
            if (cliente == null) {
                throw new NegocioException("No se encontró el cliente del presupuesto.");
            }

            Path destino = ArchivosPdf.elegirDestino(tabla.getScene().getWindow(), PresupuestoPdf.nombreSugerido(p));
            if (destino == null) {
                return;
            }

            boolean teniaDatosNegocio = Files.exists(AppPaths.getBusinessDataPath());
            try {
                PresupuestoPdf.generar(p, cliente, productos, DatosNegocio.cargar(), destino);
            } catch (IOException e) {
                e.printStackTrace();
                throw new NegocioException("No se pudo generar el PDF. Verificá que el archivo no esté abierto "
                        + "y que tengas permiso para guardar en esa carpeta.");
            }

            String aviso = "Se guardó en:\n" + destino;
            if (!teniaDatosNegocio) {
                aviso += "\n\nPara mostrar tus datos de pago y contacto en el pie del PDF, completá el archivo:\n"
                        + AppPaths.getBusinessDataPath();
            }
            Dialogos.info("PDF generado", aviso);
        });
    }

    private void abrirEditor(Presupuesto existente) {

        Dialogos.ejecutar(() -> {
            PresupuestoDialogController dialogo = Ventanas.mostrarModal("PresupuestoDialog.fxml",
                    existente == null ? "Nuevo presupuesto" : "Editar presupuesto", tabla.getScene().getWindow(),
                    (PresupuestoDialogController c) -> c.preparar(existente));

            if (dialogo.guardado() != null || dialogo.dadoDeBaja()) {
                recargar();
            }
            if (dialogo.guardado() != null) {
                presupuestos.stream()
                        .filter(p -> p.idPresupuesto().equals(dialogo.guardado().idPresupuesto()))
                        .findFirst()
                        .ifPresent(tabla.getSelectionModel()::select);
            }
        });
    }

    private void accionSobreSeleccion(Consumer<Presupuesto> accion) {

        Presupuesto p = tabla.getSelectionModel().getSelectedItem();
        if (p != null) {
            Dialogos.ejecutar(() -> accion.accept(p));
        }
    }

    private void recargar() {

        clientes.clear();
        AppContext.get().clientes().listar(false).forEach(c -> clientes.put(c.idCliente(), c));
        productos.clear();
        AppContext.get().productos().listar(false).forEach(p -> productos.put(p.idProducto(), p));

        Integer idSeleccionado = tabla.getSelectionModel().getSelectedItem() == null
                ? null : tabla.getSelectionModel().getSelectedItem().idPresupuesto();

        List<Presupuesto> lista = AppContext.get().presupuestos().listar(null).stream()
                .filter(p -> p.estado() == EstadoPresupuesto.PRESUPUESTO || p.estado() == EstadoPresupuesto.RECHAZADO)
                .filter(this::cumpleFiltro)
                .toList();
        presupuestos.setAll(lista);

        if (idSeleccionado != null) {
            presupuestos.stream()
                    .filter(p -> p.idPresupuesto().equals(idSeleccionado))
                    .findFirst()
                    .ifPresent(tabla.getSelectionModel()::select);
        }
        mostrarDetalle(tabla.getSelectionModel().getSelectedItem());
    }

    private boolean cumpleFiltro(Presupuesto p) {

        String filtro = cmbEstado.getValue();
        if (SOLO_PRESUPUESTOS.equals(filtro)) {
            return p.estado() == EstadoPresupuesto.PRESUPUESTO;
        }
        if (SOLO_RECHAZADOS.equals(filtro)) {
            return p.estado() == EstadoPresupuesto.RECHAZADO;
        }
        return true;
    }

    private void mostrarDetalle(Presupuesto p) {

        boolean hay = p != null;
        boolean abierto = hay && p.estado() == EstadoPresupuesto.PRESUPUESTO;

        lblDetalle.setText(hay ? "DETALLE DEL PRESUPUESTO N° " + p.idPresupuesto() : "DETALLE");
        tablaItems.getItems().setAll(hay ? p.items() : List.of());

        btnEditar.setDisable(!abierto);
        btnAprobar.setDisable(!abierto);
        btnRechazar.setDisable(!abierto);
        btnEliminar.setDisable(!hay);
        btnImprimir.setDisable(!hay);
    }

    private String nombreCliente(int idCliente) {
        Cliente c = clientes.get(idCliente);
        return c == null ? "(cliente eliminado)" : c.razonSocial();
    }

    private static String etiquetaEstado(EstadoPresupuesto estado) {

        return switch (estado) {
            case PRESUPUESTO -> "Presupuesto";
            case RECHAZADO -> "Rechazado";
            case ORDEN_TRABAJO -> "Orden de trabajo";
            case VENTA -> "Venta";
        };
    }
}

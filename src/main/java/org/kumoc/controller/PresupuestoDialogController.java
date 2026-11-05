package org.kumoc.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.kumoc.AppContext;
import org.kumoc.exception.NegocioException;
import org.kumoc.model.Cliente;
import org.kumoc.model.ItemPresupuesto;
import org.kumoc.model.Presupuesto;
import org.kumoc.model.Producto;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.Formato;
import org.kumoc.ui.ModoFormulario;
import org.kumoc.ui.Tablas;
import org.kumoc.ui.Ventanas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Alta y edicion de un presupuesto: cliente, productos con cantidades y total automatico. */
public class PresupuestoDialogController {

    private record Linea(Producto producto, int cantidad, BigDecimal precio) {
        BigDecimal subtotal() {
            return precio.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    @FXML
    private Label lblTitulo;
    @FXML
    private ComboBox<Cliente> cmbCliente;
    @FXML
    private ComboBox<Producto> cmbProducto;
    @FXML
    private TextField txtCantidad;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TableView<Linea> tabla;
    @FXML
    private TableColumn<Linea, String> colProducto;
    @FXML
    private TableColumn<Linea, String> colCantidad;
    @FXML
    private TableColumn<Linea, String> colPrecio;
    @FXML
    private TableColumn<Linea, String> colSubtotal;
    @FXML
    private Label lblTotal;

    private final ObservableList<Linea> lineas = FXCollections.observableArrayList();
    private Presupuesto existente;
    private Presupuesto guardado;
    private boolean dadoDeBaja;
    private ModoFormulario modo = ModoFormulario.NUEVO;

    @FXML
    private VBox panelItems;
    @FXML
    private Button btnNuevoCliente;
    @FXML
    private Button btnBaja;
    @FXML
    private Button btnPrincipal;

    @FXML
    private void initialize() {

        colProducto.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                MovimientoDialogController.descripcion(c.getValue().producto())));
        colCantidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().cantidad())));
        colPrecio.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().precio())));
        colSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().subtotal())));
        tabla.setItems(lineas);
        tabla.setPlaceholder(new Label("Todavía no agregaste productos."));
        Tablas.ajustarColumnas(tabla);

        cmbCliente.setCellFactory(lista -> celdaCliente());
        cmbCliente.setButtonCell(celdaCliente());
        cmbProducto.setCellFactory(lista -> celdaProducto());
        cmbProducto.setButtonCell(celdaProducto());

        cmbProducto.valueProperty().addListener((obs, anterior, producto) -> {
            if (producto != null) {
                txtPrecio.setText(producto.precioUnitario().toPlainString().replace('.', ','));
            }
        });
        lineas.addListener((ListChangeListener<Linea>) cambio -> actualizarTotal());
        actualizarTotal();
    }

    /** Carga los datos del dialogo; existente es null para un presupuesto nuevo. */
    public void preparar(Presupuesto existente) {

        this.existente = existente;
        cmbCliente.getItems().setAll(AppContext.get().clientes().listar(true));
        cmbProducto.getItems().setAll(AppContext.get().productos().listar(true));

        if (existente != null) {
            // El cliente puede haber sido dado de baja despues de crear el presupuesto: se mantiene visible.
            if (cmbCliente.getItems().stream().noneMatch(c -> c.idCliente().equals(existente.idCliente()))) {
                AppContext.get().clientes().buscarPorId(existente.idCliente())
                        .ifPresent(c -> cmbCliente.getItems().add(c));
            }
            lblTitulo.setText("PRESUPUESTO N° " + existente.idPresupuesto());

            Map<Integer, Producto> porId = new HashMap<>();
            AppContext.get().productos().listar(false).forEach(p -> porId.put(p.idProducto(), p));

            cmbCliente.getItems().stream()
                    .filter(c -> c.idCliente().equals(existente.idCliente()))
                    .findFirst()
                    .ifPresent(cmbCliente::setValue);
            existente.items().forEach(i -> lineas.add(new Linea(porId.get(i.idProducto()), i.cantidad(), i.precioUnitario())));
        }
        aplicarModo(existente == null ? ModoFormulario.NUEVO : ModoFormulario.VISTA);
    }

    /** Presupuesto guardado, o null si el usuario cancelo. */
    public Presupuesto guardado() {
        return guardado;
    }

    /** true si el usuario dio de baja (elimino) el presupuesto desde este dialogo. */
    public boolean dadoDeBaja() {
        return dadoDeBaja;
    }

    @FXML
    private void agregar() {

        Dialogos.ejecutar(() -> {
            Producto producto = cmbProducto.getValue();
            if (producto == null) {
                throw new NegocioException("Elegí un producto para agregar.");
            }

            int cantidad = Formato.aEntero(txtCantidad.getText(), "La cantidad");
            if (cantidad <= 0) {
                throw new NegocioException("La cantidad debe ser mayor a cero.");
            }

            BigDecimal precio = Formato.aDineroOError(txtPrecio.getText(), "El precio");
            if (precio == null) {
                precio = producto.precioUnitario();
            }
            precio = precio.setScale(2, RoundingMode.HALF_UP);
            if (precio.signum() <= 0) {
                throw new NegocioException("El precio debe ser mayor a cero.");
            }

            // Si el producto ya esta en el presupuesto, se suman las cantidades.
            for (int i = 0; i < lineas.size(); i++) {
                Linea existenteLinea = lineas.get(i);
                if (existenteLinea.producto().idProducto().equals(producto.idProducto())) {
                    lineas.set(i, new Linea(producto, existenteLinea.cantidad() + cantidad, precio));
                    reiniciarEntrada();
                    return;
                }
            }
            lineas.add(new Linea(producto, cantidad, precio));
            reiniciarEntrada();
        });
    }

    @FXML
    private void quitar() {

        Linea seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            lineas.remove(seleccionada);
        }
    }

    /** NUEVO/EDICION guardan; VISTA habilita la edicion. */
    @FXML
    private void accionPrincipal() {

        if (modo == ModoFormulario.VISTA) {
            aplicarModo(ModoFormulario.EDICION);
        } else {
            guardar();
        }
    }

    @FXML
    private void nuevoCliente() {

        Dialogos.ejecutar(() -> {
            ClienteDialogController dialogo = Ventanas.mostrarModal("ClienteDialog.fxml", "Nuevo cliente",
                    cmbCliente.getScene().getWindow(), (ClienteDialogController c) -> { });

            Cliente nuevo = dialogo.guardado();
            if (nuevo != null) {
                cmbCliente.getItems().setAll(AppContext.get().clientes().listar(true));
                cmbCliente.getItems().stream()
                        .filter(c -> c.idCliente().equals(nuevo.idCliente()))
                        .findFirst()
                        .ifPresent(cmbCliente::setValue);
            }
        });
    }

    @FXML
    private void darDeBaja() {

        if (existente == null) {
            return;
        }

        Dialogos.ejecutar(() -> {
            if (!Dialogos.confirmar("Dar de baja",
                    "¿Dar de baja el presupuesto N° " + existente.idPresupuesto()
                            + "? Se eliminará y no se puede deshacer.")) {
                return;
            }
            AppContext.get().presupuestos().eliminar(existente.idPresupuesto());
            dadoDeBaja = true;
            guardado = null;
            Ventanas.cerrar(lblTotal);
        });
    }

    private void aplicarModo(ModoFormulario nuevoModo) {

        modo = nuevoModo;
        btnPrincipal.setText(modo.textoBoton());
        boolean editable = modo.camposEditables();
        cmbCliente.setDisable(!editable);
        btnNuevoCliente.setDisable(!editable);
        panelItems.setDisable(!editable);
        btnBaja.setDisable(existente == null);
    }

    private void guardar() {

        Dialogos.ejecutar(() -> {
            Cliente cliente = cmbCliente.getValue();
            if (cliente == null) {
                throw new NegocioException("Elegí un cliente.");
            }

            List<ItemPresupuesto> items = lineas.stream()
                    .map(l -> new ItemPresupuesto(null, l.producto().idProducto(), l.cantidad(), l.precio()))
                    .toList();

            guardado = existente == null
                    ? AppContext.get().presupuestos().crear(cliente.idCliente(), items)
                    : AppContext.get().presupuestos().actualizar(existente.idPresupuesto(), cliente.idCliente(), items);
            Ventanas.cerrar(lblTotal);
        });
    }

    @FXML
    private void cancelar() {
        guardado = null;
        Ventanas.cerrar(lblTotal);
    }

    private void reiniciarEntrada() {
        cmbProducto.setValue(null);
        txtCantidad.setText("1");
        txtPrecio.clear();
    }

    private void actualizarTotal() {

        BigDecimal total = lineas.stream().map(Linea::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText(Formato.dinero(total));
    }

    private ListCell<Cliente> celdaCliente() {

        return new ListCell<>() {
            @Override
            protected void updateItem(Cliente c, boolean vacio) {
                super.updateItem(c, vacio);
                setText(vacio || c == null ? null
                        : c.cuit() == null ? c.razonSocial() : c.razonSocial() + " – " + c.cuit());
            }
        };
    }

    private ListCell<Producto> celdaProducto() {

        return new ListCell<>() {
            @Override
            protected void updateItem(Producto p, boolean vacio) {
                super.updateItem(p, vacio);
                setText(vacio || p == null ? null : MovimientoDialogController.descripcion(p));
            }
        };
    }
}

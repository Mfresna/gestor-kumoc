package org.kumoc.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.kumoc.AppContext;
import org.kumoc.model.Cliente;
import org.kumoc.model.TipoFactura;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.Tablas;

import java.util.Locale;

public class ClientesController implements Seccion {

    @FXML
    private TableView<Cliente> tabla;
    @FXML
    private TableColumn<Cliente, String> colRazonSocial;
    @FXML
    private TableColumn<Cliente, String> colCuit;
    @FXML
    private TableColumn<Cliente, String> colTipo;
    @FXML
    private TableColumn<Cliente, String> colTelefono;
    @FXML
    private TableColumn<Cliente, String> colEmail;
    @FXML
    private TextField txtBuscar;

    @FXML
    private Label lblFormTitulo;
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
    private Button btnEliminar;

    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();
    private FilteredList<Cliente> filtrados;

    /** Cliente cargado en el formulario; null si se esta creando uno nuevo. */
    private Cliente editado;

    @FXML
    private void initialize() {

        colRazonSocial.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().razonSocial()));
        colCuit.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearCuit(c.getValue().cuit())));
        colTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().tipoFactura().name()));
        colTelefono.setCellValueFactory(c -> new ReadOnlyStringWrapper(vacio(c.getValue().telefono())));
        colEmail.setCellValueFactory(c -> new ReadOnlyStringWrapper(vacio(c.getValue().email())));
        tabla.setPlaceholder(new Label("No hay clientes para mostrar."));
        Tablas.ajustarColumnas(tabla);

        cmbTipoFactura.getItems().setAll(TipoFactura.values());

        filtrados = new FilteredList<>(clientes, c -> true);
        tabla.setItems(filtrados);
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> filtrar(texto));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, cliente) -> {
            if (cliente != null) {
                cargarEnFormulario(cliente);
            }
        });

        limpiarFormulario();
    }

    @Override
    public void alMostrar() {
        recargar();
    }

    @FXML
    private void nuevo() {
        tabla.getSelectionModel().clearSelection();
        limpiarFormulario();
    }

    @FXML
    private void guardar() {

        Dialogos.ejecutar(() -> {
            Cliente guardado = AppContext.get().clientes().guardar(new Cliente(
                    editado == null ? null : editado.idCliente(),
                    txtRazonSocial.getText(), txtCuit.getText(), cmbTipoFactura.getValue(),
                    txtEmail.getText(), txtTelefono.getText(), txtDireccion.getText()));

            editado = guardado;
            recargar();
            seleccionar(guardado.idCliente());
        });
    }

    @FXML
    private void eliminar() {

        if (editado == null) {
            return;
        }

        Dialogos.ejecutar(() -> {
            if (!Dialogos.confirmar("Eliminar cliente",
                    "¿Eliminar a \"" + editado.razonSocial() + "\"? Esta acción no se puede deshacer.")) {
                return;
            }
            AppContext.get().clientes().eliminar(editado.idCliente());
            tabla.getSelectionModel().clearSelection();
            limpiarFormulario();
            recargar();
        });
    }

    private void recargar() {

        Integer idSeleccionado = editado == null ? null : editado.idCliente();
        clientes.setAll(AppContext.get().clientes().listar());

        if (idSeleccionado != null) {
            seleccionar(idSeleccionado);
        }
    }

    private void seleccionar(int idCliente) {

        clientes.stream()
                .filter(c -> c.idCliente() == idCliente)
                .findFirst()
                .ifPresentOrElse(c -> {
                    tabla.getSelectionModel().select(c);
                    cargarEnFormulario(c);
                }, this::limpiarFormulario);
    }

    private void filtrar(String texto) {

        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        filtrados.setPredicate(c -> filtro.isEmpty()
                || c.razonSocial().toLowerCase(Locale.ROOT).contains(filtro)
                || c.cuit().contains(filtro.replaceAll("[\\s-]", ""))
                || vacio(c.telefono()).toLowerCase(Locale.ROOT).contains(filtro));
    }

    private void cargarEnFormulario(Cliente cliente) {

        editado = cliente;
        lblFormTitulo.setText("EDITAR CLIENTE");
        txtRazonSocial.setText(cliente.razonSocial());
        txtCuit.setText(formatearCuit(cliente.cuit()));
        cmbTipoFactura.setValue(cliente.tipoFactura());
        txtTelefono.setText(vacio(cliente.telefono()));
        txtEmail.setText(vacio(cliente.email()));
        txtDireccion.setText(vacio(cliente.direccion()));
        btnEliminar.setDisable(false);
    }

    private void limpiarFormulario() {

        editado = null;
        lblFormTitulo.setText("NUEVO CLIENTE");
        txtRazonSocial.clear();
        txtCuit.clear();
        cmbTipoFactura.setValue(TipoFactura.X);
        txtTelefono.clear();
        txtEmail.clear();
        txtDireccion.clear();
        btnEliminar.setDisable(true);
    }

    /** 20123456789 -> 20-12345678-9 */
    private static String formatearCuit(String cuit) {

        return cuit.length() == 11
                ? cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10)
                : cuit;
    }

    private static String vacio(String valor) {
        return valor == null ? "" : valor;
    }
}

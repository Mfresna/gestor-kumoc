package org.kumoc.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.kumoc.AppContext;
import org.kumoc.exception.NegocioException;
import org.kumoc.model.Producto;
import org.kumoc.ui.Dialogos;
import org.kumoc.ui.EstadoApp;
import org.kumoc.ui.Formato;
import org.kumoc.ui.Imagenes;
import org.kumoc.ui.ModoFormulario;
import org.kumoc.ui.Tablas;

import java.io.File;
import java.math.BigDecimal;
import java.util.Locale;

public class ProductosController implements Seccion {

    @FXML
    private TableView<Producto> tabla;
    @FXML
    private TableColumn<Producto, Producto> colImagen;
    @FXML
    private TableColumn<Producto, String> colCodigo;
    @FXML
    private TableColumn<Producto, String> colNombre;
    @FXML
    private TableColumn<Producto, String> colTalle;
    @FXML
    private TableColumn<Producto, String> colColor;
    @FXML
    private TableColumn<Producto, String> colPrecio;
    @FXML
    private TableColumn<Producto, String> colMinimo;
    @FXML
    private TableColumn<Producto, String> colEstado;
    @FXML
    private TextField txtBuscar;
    @FXML
    private CheckBox chkBajas;

    @FXML
    private Label lblFormTitulo;
    @FXML
    private ImageView imgPreview;
    @FXML
    private Label lblSinImagen;
    @FXML
    private Button btnQuitarImagen;
    @FXML
    private Button btnElegirImagen;
    @FXML
    private Button btnPrincipal;
    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtTalle;
    @FXML
    private TextField txtColor;
    @FXML
    private TextField txtPublico;
    @FXML
    private TextArea txtDescripcion;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TextField txtMinimo;
    @FXML
    private Button btnBaja;

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private FilteredList<Producto> filtrados;

    /** Producto cargado en el formulario; null si se esta creando uno nuevo. */
    private Producto editado;
    private ModoFormulario modo = ModoFormulario.NUEVO;
    /** Imagen mostrada en el formulario (guardada o recien elegida). */
    private String imagenForm;
    /** Imagen elegida en esta edicion que todavia no esta guardada en un producto. */
    private String imagenNueva;

    @FXML
    private void initialize() {

        colImagen.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colImagen.setCellFactory(columna -> new TableCell<>() {
            private final ImageView vista = new ImageView();

            {
                vista.setFitWidth(28);
                vista.setFitHeight(28);
                vista.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Producto producto, boolean vacio) {
                super.updateItem(producto, vacio);
                Image miniatura = vacio || producto == null ? null : Imagenes.miniatura(producto.imagen(), 56);
                vista.setImage(miniatura);
                setGraphic(miniatura == null ? null : vista);
            }
        });

        colCodigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(vacio(c.getValue().codigo())));
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colTalle.setCellValueFactory(c -> new ReadOnlyStringWrapper(vacio(c.getValue().talle())));
        colColor.setCellValueFactory(c -> new ReadOnlyStringWrapper(vacio(c.getValue().color())));
        colPrecio.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().precioUnitario())));
        colMinimo.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().stockMinimo())));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().activo() ? "Activo" : "De baja"));

        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Producto producto, boolean vacio) {
                super.updateItem(producto, vacio);
                getStyleClass().remove("inactivo");
                if (!vacio && producto != null && !producto.activo()) {
                    getStyleClass().add("inactivo");
                }
            }
        });
        tabla.setPlaceholder(new Label("No hay productos para mostrar."));
        Tablas.ajustarColumnas(tabla);

        filtrados = new FilteredList<>(productos, p -> true);
        tabla.setItems(filtrados);
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> filtrar(texto));
        chkBajas.selectedProperty().addListener((obs, anterior, valor) -> Dialogos.ejecutar(this::recargar));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, producto) -> {
            if (producto != null) {
                cargarEnFormulario(producto);
            }
        });

        limpiarFormulario();
    }

    @Override
    public void alMostrar() {
        recargar();
    }

    @Override
    public void alSalir() {
        descartarImagenNueva();
    }

    @FXML
    private void nuevo() {
        tabla.getSelectionModel().clearSelection();
        limpiarFormulario();
    }

    /** NUEVO/EDICION guardan; VISTA habilita los campos para editar. */
    @FXML
    private void accionPrincipal() {

        if (modo == ModoFormulario.VISTA) {
            aplicarModo(ModoFormulario.EDICION);
        } else {
            guardar();
        }
    }

    private void aplicarModo(ModoFormulario nuevoModo) {

        modo = nuevoModo;
        btnPrincipal.setText(modo.textoBoton());
        boolean editable = modo.camposEditables();
        txtCodigo.setDisable(!editable);
        txtNombre.setDisable(!editable);
        txtTalle.setDisable(!editable);
        txtColor.setDisable(!editable);
        txtPublico.setDisable(!editable);
        txtDescripcion.setDisable(!editable);
        txtPrecio.setDisable(!editable);
        txtMinimo.setDisable(!editable);
        btnElegirImagen.setDisable(!editable);
        btnQuitarImagen.setDisable(!editable || imagenForm == null);
    }

    private void guardar() {

        Dialogos.ejecutar(() -> {
            Producto aGuardar = new Producto(
                    editado == null ? null : editado.idProducto(),
                    txtCodigo.getText(), txtNombre.getText(), txtTalle.getText(), txtPublico.getText(),
                    txtColor.getText(), txtDescripcion.getText(), leerPrecio(),
                    editado == null || editado.activo(), leerMinimo(), imagenForm);

            Producto guardado = AppContext.get().productos().guardar(aGuardar);

            if (editado != null && editado.imagen() != null && !editado.imagen().equals(guardado.imagen())) {
                Imagenes.olvidar(editado.imagen());
            }
            imagenNueva = null;

            recargar();
            seleccionar(guardado.idProducto());
            EstadoApp.refrescarAlertas();
        });
    }

    @FXML
    private void cambiarBaja() {

        if (editado == null) {
            return;
        }

        Dialogos.ejecutar(() -> {
            int id = editado.idProducto();
            if (editado.activo()) {
                if (!Dialogos.confirmar("Dar de baja",
                        "El producto \"" + editado.nombre() + "\" dejará de ofrecerse en nuevos presupuestos. "
                                + "Su historial se conserva y podés reactivarlo.")) {
                    return;
                }
                AppContext.get().productos().darDeBaja(id);
            } else {
                AppContext.get().productos().reactivar(id);
            }
            recargar();
            seleccionar(id);
            EstadoApp.refrescarAlertas();
        });
    }

    @FXML
    private void elegirImagen() {

        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir imagen del producto");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));

        File archivo = selector.showOpenDialog(tabla.getScene().getWindow());
        if (archivo == null) {
            return;
        }

        Dialogos.ejecutar(() -> {
            String nombre = AppContext.get().imagenes().guardar(archivo.toPath());
            descartarImagenNueva();
            imagenNueva = nombre;
            imagenForm = nombre;
            mostrarImagen();
        });
    }

    @FXML
    private void quitarImagen() {
        descartarImagenNueva();
        imagenForm = null;
        mostrarImagen();
    }

    private void recargar() {

        Integer idSeleccionado = editado == null ? null : editado.idProducto();
        productos.setAll(AppContext.get().productos().listar(!chkBajas.isSelected()));

        if (idSeleccionado != null) {
            seleccionar(idSeleccionado);
        }
    }

    private void seleccionar(int idProducto) {

        productos.stream()
                .filter(p -> p.idProducto() == idProducto)
                .findFirst()
                .ifPresentOrElse(p -> {
                    tabla.getSelectionModel().select(p);
                    cargarEnFormulario(p);
                }, this::limpiarFormulario);
    }

    private void filtrar(String texto) {

        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        filtrados.setPredicate(p -> filtro.isEmpty()
                || vacio(p.codigo()).toLowerCase(Locale.ROOT).contains(filtro)
                || p.nombre().toLowerCase(Locale.ROOT).contains(filtro)
                || vacio(p.talle()).toLowerCase(Locale.ROOT).contains(filtro)
                || vacio(p.color()).toLowerCase(Locale.ROOT).contains(filtro));
    }

    private void cargarEnFormulario(Producto producto) {

        if (editado == null || !editado.idProducto().equals(producto.idProducto())) {
            descartarImagenNueva();
        }

        editado = producto;
        imagenForm = producto.imagen();
        lblFormTitulo.setText("EDITAR PRODUCTO");
        txtCodigo.setText(vacio(producto.codigo()));
        txtNombre.setText(producto.nombre());
        txtTalle.setText(vacio(producto.talle()));
        txtColor.setText(vacio(producto.color()));
        txtPublico.setText(vacio(producto.publicoObjetivo()));
        txtDescripcion.setText(vacio(producto.descripcion()));
        txtPrecio.setText(producto.precioUnitario().toPlainString().replace('.', ','));
        txtMinimo.setText(String.valueOf(producto.stockMinimo()));
        btnBaja.setText(producto.activo() ? "DAR DE BAJA" : "REACTIVAR");
        btnBaja.setDisable(false);
        mostrarImagen();
        aplicarModo(ModoFormulario.VISTA);
    }

    private void limpiarFormulario() {

        descartarImagenNueva();
        editado = null;
        imagenForm = null;
        lblFormTitulo.setText("NUEVO PRODUCTO");
        txtCodigo.clear();
        txtNombre.clear();
        txtTalle.clear();
        txtColor.clear();
        txtPublico.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        txtMinimo.setText("0");
        btnBaja.setText("DAR DE BAJA");
        btnBaja.setDisable(true);
        mostrarImagen();
        aplicarModo(ModoFormulario.NUEVO);
    }

    private void mostrarImagen() {

        Image imagen = imagenForm == null ? null : Imagenes.completa(imagenForm);
        imgPreview.setImage(imagen);
        lblSinImagen.setVisible(imagen == null);
        btnQuitarImagen.setDisable(imagen == null);
    }

    /** Borra la imagen elegida en esta edicion si no llego a guardarse en un producto. */
    private void descartarImagenNueva() {

        if (imagenNueva != null) {
            AppContext.get().imagenes().eliminar(imagenNueva);
            Imagenes.olvidar(imagenNueva);
            imagenNueva = null;
        }
    }

    private BigDecimal leerPrecio() {

        try {
            BigDecimal precio = Formato.aDinero(txtPrecio.getText());
            if (precio == null) {
                throw new NegocioException("El precio es obligatorio.");
            }
            return precio;
        } catch (NumberFormatException e) {
            throw new NegocioException("El precio no es un número válido.");
        }
    }

    private int leerMinimo() {

        String texto = txtMinimo.getText() == null ? "" : txtMinimo.getText().trim();
        if (texto.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new NegocioException("El stock mínimo debe ser un número entero.");
        }
    }

    private static String vacio(String valor) {
        return valor == null ? "" : valor;
    }
}

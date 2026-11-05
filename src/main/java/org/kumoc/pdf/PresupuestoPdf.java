package org.kumoc.pdf;

import org.kumoc.config.DatosNegocio;
import org.kumoc.model.Cliente;
import org.kumoc.model.ItemPresupuesto;
import org.kumoc.model.Presupuesto;
import org.kumoc.model.Producto;
import org.kumoc.ui.Formato;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Presupuesto en PDF con el logo de KUMOC: cabecera, datos del cliente, detalle, total y pie con datos del negocio. */
public final class PresupuestoPdf extends PdfBase {

    private static final float COL_DESC = MARGEN + 10;
    private static final float COL_DESC_ANCHO = 235;
    private static final float COL_CANT = 345;
    private static final float COL_PRECIO = 448;
    private static final float COL_SUBTOTAL = DERECHA - 10;
    private static final float ALTO_PIE = 95;

    private final Presupuesto presupuesto;
    private final String titulo;
    private float limitePie = 60;

    private PresupuestoPdf(Presupuesto presupuesto) throws IOException {
        this.presupuesto = presupuesto;
        this.titulo = "PRESUPUESTO N° " + numero(presupuesto);
    }

    /** Genera el PDF del presupuesto en el destino indicado (lo sobrescribe si existe). */
    public static void generar(Presupuesto presupuesto, Cliente cliente, Map<Integer, Producto> productos,
                               DatosNegocio negocio, Path destino) throws IOException {

        try (PresupuestoPdf pdf = new PresupuestoPdf(presupuesto)) {
            pdf.construir(cliente, productos, negocio);
            pdf.guardar(destino);
        }
    }

    /** Nombre sugerido para el archivo, por ejemplo Presupuesto-00000012.pdf. */
    public static String nombreSugerido(Presupuesto presupuesto) {
        return "Presupuesto-" + numero(presupuesto) + ".pdf";
    }

    private static String numero(Presupuesto p) {
        return String.format("%08d", p.idPresupuesto());
    }

    private void construir(Cliente cliente, Map<Integer, Producto> productos, DatosNegocio negocio) throws IOException {

        boolean hayPie = !negocio.lineasPago().isEmpty() || !negocio.lineasContacto().isEmpty();
        limitePie = hayPie ? ALTO_PIE + 45 : 60;

        primeraPagina("PRESUPUESTO", "N°: " + numero(presupuesto),
                presupuesto.fechaEmision().format(FECHA_LARGA));

        datosCliente(cliente);
        y -= 28;
        encabezadoTabla();
        for (ItemPresupuesto item : presupuesto.items()) {
            fila(item, productos.get(item.idProducto()));
        }
        total();
        if (hayPie) {
            pie(negocio);
        }
    }

    private void datosCliente(Cliente cliente) throws IOException {

        escribir("Datos del cliente:", MARGEN, y, negrita, 15, NEGRO);
        y -= 24;

        List<String> lineas = new ArrayList<>();
        lineas.add(cliente.razonSocial());
        if (cliente.cuit() != null) {
            lineas.add("CUIT/CUIL: " + formatearCuit(cliente.cuit()));
        }
        if (cliente.telefono() != null) {
            lineas.add("Tel.: " + cliente.telefono());
        }
        if (cliente.email() != null) {
            lineas.add(cliente.email());
        }
        if (cliente.direccion() != null) {
            lineas.add(cliente.direccion());
        }
        String ubicacion = ubicacion(cliente);
        if (!ubicacion.isEmpty()) {
            lineas.add(ubicacion);
        }
        for (String linea : lineas) {
            escribir(linea, MARGEN, y, normal, 11.5f, TEXTO);
            y -= 17;
        }
    }

    private void encabezadoTabla() throws IOException {

        float alto = 30;
        rellenar(MARGEN, y - alto, DERECHA - MARGEN, alto, GRIS_FONDO);
        float base = y - 19;
        escribir("Descripción", COL_DESC, base, normal, 11, GRIS_TEXTO);
        escribirDerecha("Cant.", COL_CANT, base, normal, 11, GRIS_TEXTO);
        escribirDerecha("Precio unit.", COL_PRECIO, base, normal, 11, GRIS_TEXTO);
        escribirDerecha("Subtotal", COL_SUBTOTAL, base, normal, 11, GRIS_TEXTO);
        y -= alto;
    }

    private void fila(ItemPresupuesto item, Producto producto) throws IOException {

        String nombre = producto == null
                ? "Producto #" + item.idProducto()
                : producto.codigo() == null ? producto.nombre() : producto.codigo() + " – " + producto.nombre();
        List<String> lineasNombre = partir(nombre, normal, 10.5f, COL_DESC_ANCHO);
        String detalle = producto == null ? "" : detalleProducto(producto);

        float alto = 12 * lineasNombre.size() + (detalle.isEmpty() ? 0 : 11) + 16;

        if (asegurarEspacio(alto, titulo)) {
            encabezadoTabla();
        }

        float base = y - 17;
        for (String linea : lineasNombre) {
            escribir(linea, COL_DESC, base, normal, 10.5f, TEXTO);
            base -= 12;
        }
        if (!detalle.isEmpty()) {
            escribir(detalle, COL_DESC, base, normal, 9, GRIS_TEXTO);
        }

        float centro = y - 17;
        escribirDerecha(String.valueOf(item.cantidad()), COL_CANT, centro, normal, 10.5f, TEXTO);
        escribirDerecha(Formato.dinero(item.precioUnitario()), COL_PRECIO, centro, normal, 10.5f, TEXTO);
        escribirDerecha(Formato.dinero(item.subtotal()), COL_SUBTOTAL, centro, normal, 10.5f, TEXTO);

        y -= alto;
        linea(MARGEN, DERECHA, y, GRIS_LINEA);
    }

    private void total() throws IOException {

        // El total y el pie van juntos en la ultima pagina: se reserva el espacio del pie solo para ellos.
        limiteInferior = limitePie;
        asegurarEspacio(60, titulo);
        y -= 26;

        float cajaAncho = 225;
        float cajaAlto = 40;
        float cajaX = DERECHA - cajaAncho;
        rellenar(cajaX, y - cajaAlto, cajaAncho, cajaAlto, GRIS_FONDO);

        float base = y - 26;
        escribir("TOTAL:", cajaX + 16, base, negrita, 15, TEXTO);
        escribirDerecha(Formato.dinero(presupuesto.total()), DERECHA - 12, base, negrita, 15, TEXTO);
        y -= cajaAlto;
    }

    private void pie(DatosNegocio negocio) throws IOException {

        float base = 30;
        rellenar(0, base, ANCHO, ALTO_PIE, GRIS_FONDO);

        columnaPie("INFORMACIÓN PARA EL PAGO", negocio.lineasPago(), MARGEN + 10, base);
        columnaPie("DATOS DE CONTACTO", negocio.lineasContacto(), ANCHO / 2 + 10, base);
    }

    private void columnaPie(String encabezado, List<String> lineas, float x, float baseBanda) throws IOException {

        if (lineas.isEmpty()) {
            return;
        }
        float yy = baseBanda + ALTO_PIE - 26;
        escribir(encabezado, x, yy, negrita, 11, NEGRO);
        for (String linea : lineas) {
            yy -= 15;
            escribir(linea, x, yy, normal, 10, TEXTO);
        }
    }

    /** "Localidad (CP 5000), Provincia" con lo que este cargado. */
    private static String ubicacion(Cliente c) {

        String texto = c.localidad() == null ? "" : c.localidad();
        if (c.codigoPostal() != null) {
            String cp = "CP " + c.codigoPostal();
            texto = texto.isEmpty() ? cp : texto + " (" + cp + ")";
        }
        if (c.provincia() != null) {
            texto = texto.isEmpty() ? c.provincia() : texto + ", " + c.provincia();
        }
        return texto;
    }

    private static String detalleProducto(Producto p) {

        List<String> partes = new ArrayList<>();
        if (p.talle() != null && !p.talle().isBlank()) {
            partes.add("Talle " + p.talle());
        }
        if (p.color() != null && !p.color().isBlank()) {
            partes.add("Color " + p.color());
        }
        return String.join(" · ", partes);
    }

    /** 20123456789 -> 20-12345678-9 */
    private static String formatearCuit(String cuit) {

        return cuit.length() == 11
                ? cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10)
                : cuit;
    }
}

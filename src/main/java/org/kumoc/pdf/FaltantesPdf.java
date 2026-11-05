package org.kumoc.pdf;

import org.kumoc.model.FaltanteFabricacion;
import org.kumoc.model.Producto;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Listado en PDF de lo que falta fabricar para cubrir las ordenes en produccion. */
public final class FaltantesPdf extends PdfBase {

    private static final String TITULO = "FALTANTES A FABRICAR";
    private static final float COL_PRODUCTO = MARGEN + 10;
    private static final float COL_PRODUCTO_ANCHO = 205;
    private static final float COL_PEDIDO = 312;
    private static final float COL_STOCK = 362;
    private static final float COL_FABRICAR = 432;
    private static final float COL_ORDENES = 445;
    private static final float COL_ORDENES_ANCHO = DERECHA - 8 - COL_ORDENES;

    private FaltantesPdf() throws IOException {
    }

    public static void generar(List<FaltanteFabricacion> faltantes, Path destino) throws IOException {

        try (FaltantesPdf pdf = new FaltantesPdf()) {
            pdf.construir(faltantes);
            pdf.guardar(destino);
        }
    }

    public static String nombreSugerido() {
        return "Faltantes-a-fabricar-" + LocalDate.now() + ".pdf";
    }

    private void construir(List<FaltanteFabricacion> faltantes) throws IOException {

        primeraPagina(TITULO, "Órdenes de trabajo en producción", LocalDate.now().format(FECHA_LARGA));

        if (faltantes.isEmpty()) {
            escribir("No hay nada pendiente de fabricar: el stock cubre todas las órdenes en producción.",
                    MARGEN, y, normal, 12, TEXTO);
            return;
        }

        encabezadoTabla();
        int totalUnidades = 0;
        for (FaltanteFabricacion f : faltantes) {
            fila(f);
            totalUnidades += f.aFabricar();
        }

        asegurarEspacio(50, TITULO);
        y -= 26;
        float cajaAncho = 250;
        float cajaAlto = 36;
        float cajaX = DERECHA - cajaAncho;
        rellenar(cajaX, y - cajaAlto, cajaAncho, cajaAlto, GRIS_FONDO);
        float base = y - 23;
        escribir("TOTAL A FABRICAR:", cajaX + 14, base, negrita, 12, TEXTO);
        escribirDerecha(totalUnidades + " u.", DERECHA - 12, base, negrita, 14, TEXTO);
        y -= cajaAlto;
    }

    private void encabezadoTabla() throws IOException {

        float alto = 30;
        rellenar(MARGEN, y - alto, DERECHA - MARGEN, alto, GRIS_FONDO);
        float base = y - 19;
        escribir("Producto", COL_PRODUCTO, base, normal, 11, GRIS_TEXTO);
        escribirDerecha("Pedido", COL_PEDIDO, base, normal, 11, GRIS_TEXTO);
        escribirDerecha("Stock", COL_STOCK, base, normal, 11, GRIS_TEXTO);
        escribirDerecha("A fabricar", COL_FABRICAR, base, normal, 11, GRIS_TEXTO);
        escribir("Órdenes", COL_ORDENES, base, normal, 11, GRIS_TEXTO);
        y -= alto;
    }

    private void fila(FaltanteFabricacion f) throws IOException {

        Producto p = f.producto();
        List<String> lineasNombre = partir(p.codigo() == null ? p.nombre() : p.codigo() + " – " + p.nombre(), normal, 10.5f, COL_PRODUCTO_ANCHO);
        String detalle = detalle(p);
        String ordenes = f.ordenes().stream().map(n -> "N° " + n).collect(Collectors.joining(", "));
        List<String> lineasOrdenes = partir(ordenes, normal, 9.5f, COL_ORDENES_ANCHO);

        int lineas = Math.max(lineasNombre.size() + (detalle.isEmpty() ? 0 : 1), lineasOrdenes.size());
        float alto = 12 * lineas + 16;

        if (asegurarEspacio(alto, TITULO)) {
            encabezadoTabla();
        }

        float base = y - 17;
        for (String linea : lineasNombre) {
            escribir(linea, COL_PRODUCTO, base, normal, 10.5f, TEXTO);
            base -= 12;
        }
        if (!detalle.isEmpty()) {
            escribir(detalle, COL_PRODUCTO, base, normal, 9, GRIS_TEXTO);
        }

        float centro = y - 17;
        escribirDerecha(String.valueOf(f.pedido()), COL_PEDIDO, centro, normal, 10.5f, TEXTO);
        escribirDerecha(String.valueOf(f.fisico()), COL_STOCK, centro, normal, 10.5f, TEXTO);
        escribirDerecha(String.valueOf(f.aFabricar()), COL_FABRICAR, centro, negrita, 11.5f, NEGRO);

        float baseOrdenes = y - 17;
        for (String linea : lineasOrdenes) {
            escribir(linea, COL_ORDENES, baseOrdenes, normal, 9.5f, GRIS_TEXTO);
            baseOrdenes -= 12;
        }

        y -= alto;
        linea(MARGEN, DERECHA, y, GRIS_LINEA);
    }

    private static String detalle(Producto p) {

        List<String> partes = new ArrayList<>();
        if (p.talle() != null && !p.talle().isBlank()) {
            partes.add("Talle " + p.talle());
        }
        if (p.color() != null && !p.color().isBlank()) {
            partes.add("Color " + p.color());
        }
        return String.join(" · ", partes);
    }
}

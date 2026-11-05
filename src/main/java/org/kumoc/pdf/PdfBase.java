package org.kumoc.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Base comun de los PDF de KUMOC: pagina A4, franja negra superior, recuadro con el logo, fuentes,
 * helpers de dibujo y salto de pagina. Los textos se limpian para la codificacion WinAnsi de Helvetica
 * (cubre tildes y la enie).
 */
abstract class PdfBase implements AutoCloseable {

    static final Color NEGRO = Color.BLACK;
    static final Color TEXTO = new Color(0x1F1F1F);
    static final Color GRIS_TEXTO = new Color(0x555555);
    static final Color GRIS_FONDO = new Color(0xF1F1F1);
    static final Color GRIS_LINEA = new Color(0xDDDDDD);

    static final float ANCHO = PDRectangle.A4.getWidth();
    static final float ALTO = PDRectangle.A4.getHeight();
    static final float MARGEN = 45;
    static final float DERECHA = ANCHO - MARGEN;

    static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-AR"));

    final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    final PDType1Font negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final PDDocument documento = new PDDocument();
    private final PDImageXObject logo;
    private PDPageContentStream flujo;

    /** Coordenada Y actual (crece hacia abajo en la pagina; se mide desde el borde inferior). */
    float y;
    /** Limite inferior del contenido en cada pagina; lo define la subclase (por ejemplo, reserva el pie). */
    float limiteInferior = 60;

    PdfBase() throws IOException {

        try (InputStream in = PdfBase.class.getResourceAsStream("/img/logo.png")) {
            if (in == null) {
                throw new IOException("No se encontró el logo de KUMOC.");
            }
            logo = PDImageXObject.createFromByteArray(documento, in.readAllBytes(), "logo");
        }
    }

    // --- Paginas ---

    /** Primera pagina: franja, titulo grande, lineas bajo el titulo y recuadro con el logo. */
    void primeraPagina(String titulo, String linea1, String linea2) throws IOException {

        abrirPagina();
        // El titulo se achica si no entra a la izquierda del recuadro del logo.
        float tamanoTitulo = 32;
        float anchoDisponible = 295;
        while (tamanoTitulo > 16 && ancho(titulo, negrita, tamanoTitulo) > anchoDisponible) {
            tamanoTitulo -= 1;
        }
        escribir(titulo, MARGEN, ALTO - 92, negrita, tamanoTitulo, NEGRO);
        escribir(linea1, MARGEN, ALTO - 114, normal, 13, TEXTO);
        escribir(linea2, MARGEN, ALTO - 132, normal, 13, TEXTO);

        // Recuadro negro con el logo (el logo es blanco sobre negro).
        float cajaAncho = 190;
        float cajaAlto = 84;
        float cajaX = DERECHA - cajaAncho;
        float cajaY = ALTO - 50 - cajaAlto;
        rellenar(cajaX, cajaY, cajaAncho, cajaAlto, NEGRO);

        float logoAncho = 150;
        float logoAlto = logoAncho * logo.getHeight() / logo.getWidth();
        flujo.drawImage(logo, cajaX + (cajaAncho - logoAncho) / 2, cajaY + (cajaAlto - logoAlto) / 2,
                logoAncho, logoAlto);

        y = ALTO - 175;
    }

    /** Paginas siguientes: franja y una linea de continuacion. */
    void paginaContinuacion(String titulo) throws IOException {

        abrirPagina();
        escribir(titulo + " (continuación)", MARGEN, ALTO - 50, negrita, 12, TEXTO);
        y = ALTO - 80;
    }

    /** Pasa a una pagina nueva si no entra una altura dada; devuelve true si salto. */
    boolean asegurarEspacio(float altura, String tituloContinuacion) throws IOException {

        if (y - altura >= limiteInferior) {
            return false;
        }
        paginaContinuacion(tituloContinuacion);
        return true;
    }

    private void abrirPagina() throws IOException {

        if (flujo != null) {
            flujo.close();
        }
        PDPage pagina = new PDPage(PDRectangle.A4);
        documento.addPage(pagina);
        flujo = new PDPageContentStream(documento, pagina);
        rellenar(0, ALTO - 16, ANCHO, 16, NEGRO);
    }

    void guardar(Path destino) throws IOException {

        flujo.close();
        flujo = null;
        documento.save(destino.toFile());
    }

    @Override
    public void close() throws IOException {

        if (flujo != null) {
            flujo.close();
            flujo = null;
        }
        documento.close();
    }

    // --- Dibujo ---

    void rellenar(float x, float yy, float ancho, float alto, Color color) throws IOException {
        flujo.setNonStrokingColor(color);
        flujo.addRect(x, yy, ancho, alto);
        flujo.fill();
    }

    void linea(float x1, float x2, float yy, Color color) throws IOException {
        flujo.setStrokingColor(color);
        flujo.setLineWidth(0.8f);
        flujo.moveTo(x1, yy);
        flujo.lineTo(x2, yy);
        flujo.stroke();
    }

    void escribir(String texto, float x, float yy, PDType1Font fuente, float tamano, Color color) throws IOException {

        String limpio = limpiar(texto, fuente);
        if (limpio.isEmpty()) {
            return;
        }
        flujo.setNonStrokingColor(color);
        flujo.beginText();
        flujo.setFont(fuente, tamano);
        flujo.newLineAtOffset(x, yy);
        flujo.showText(limpio);
        flujo.endText();
    }

    void escribirDerecha(String texto, float xDerecha, float yy, PDType1Font fuente, float tamano, Color color)
            throws IOException {
        escribir(texto, xDerecha - ancho(texto, fuente, tamano), yy, fuente, tamano, color);
    }

    float ancho(String texto, PDType1Font fuente, float tamano) throws IOException {
        return fuente.getStringWidth(limpiar(texto, fuente)) / 1000f * tamano;
    }

    /** Parte el texto en lineas que no superen el ancho maximo (corta por palabras). */
    List<String> partir(String texto, PDType1Font fuente, float tamano, float anchoMaximo) throws IOException {

        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();

        for (String palabra : limpiar(texto, fuente).split("\\s+")) {
            String candidata = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (!actual.isEmpty() && ancho(candidata, fuente, tamano) > anchoMaximo) {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(candidata);
            }
        }
        if (!actual.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    /** Reemplaza por '?' los caracteres que la fuente no puede codificar y normaliza espacios raros. */
    private static String limpiar(String texto, PDType1Font fuente) {

        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : texto.replace(' ', ' ').replace(' ', ' ').toCharArray()) {
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
                continue;
            }
            try {
                fuente.encode(String.valueOf(c));
                sb.append(c);
            } catch (IllegalArgumentException | IOException e) {
                sb.append('?');
            }
        }
        return sb.toString().trim();
    }
}

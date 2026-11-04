package org.kumoc.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.kumoc.AppContext;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Convierte imagenes guardadas a JavaFX. Las miniaturas se cachean por nombre de archivo. */
public final class Imagenes {

    private static final Map<String, Image> MINIATURAS = new HashMap<>();

    private Imagenes() {
    }

    /** Imagen completa (hasta 800 px) o null si no hay imagen. */
    public static Image completa(String nombre) {
        return AppContext.get().imagenes().leer(nombre).map(Imagenes::aFx).orElse(null);
    }

    /** Miniatura cuadrada acotada a lado px, o null si no hay imagen. */
    public static Image miniatura(String nombre, int lado) {

        if (nombre == null) {
            return null;
        }
        String clave = nombre + "@" + lado;
        if (!MINIATURAS.containsKey(clave)) {
            Optional<BufferedImage> original = AppContext.get().imagenes().leer(nombre);
            MINIATURAS.put(clave, original.map(i -> aFx(reducir(i, lado))).orElse(null));
        }
        return MINIATURAS.get(clave);
    }

    /** Descarta las miniaturas en cache de una imagen que cambio o se elimino. */
    public static void olvidar(String nombre) {
        MINIATURAS.keySet().removeIf(clave -> clave.startsWith(nombre + "@"));
    }

    private static BufferedImage reducir(BufferedImage original, int lado) {

        double escala = Math.min(1.0, (double) lado / Math.max(original.getWidth(), original.getHeight()));
        int ancho = Math.max(1, (int) Math.round(original.getWidth() * escala));
        int alto = Math.max(1, (int) Math.round(original.getHeight() * escala));

        BufferedImage resultado = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resultado.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(original, 0, 0, ancho, alto, null);
        } finally {
            g.dispose();
        }
        return resultado;
    }

    /** Copia los pixeles a una imagen JavaFX (evita depender de javafx-swing). */
    public static Image aFx(BufferedImage imagen) {

        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        int[] pixeles = imagen.getRGB(0, 0, ancho, alto, null, 0, ancho);

        WritableImage resultado = new WritableImage(ancho, alto);
        resultado.getPixelWriter().setPixels(0, 0, ancho, alto, PixelFormat.getIntArgbInstance(), pixeles, 0, ancho);
        return resultado;
    }
}

package org.kumoc.service;

import org.kumoc.exception.AccesoDatosException;
import org.kumoc.exception.NegocioException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guarda las imagenes de producto comprimidas (WebP con perdida, maximo 800 px de lado) en una
 * carpeta propia. En la base solo se guarda el nombre de archivo generado ({uuid}.webp).
 */
public class ImagenService {

    private static final long MAX_BYTES_ORIGEN = 20L * 1024 * 1024;
    private static final long MAX_PIXELES_ORIGEN = 50_000_000L;
    private static final int LADO_MAXIMO = 800;
    private static final float CALIDAD = 0.8f;
    private static final Set<String> EXTENSIONES = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[0-9a-f\\-]{36}\\.webp");

    private final Path carpeta;

    public ImagenService(Path carpeta) {
        this.carpeta = carpeta;
    }

    /** Comprime la imagen de origen, la guarda en la carpeta y devuelve el nombre de archivo generado. */
    public String guardar(Path origen) {

        validarOrigen(origen);
        BufferedImage reducida = reducir(leerOrigen(origen));
        String nombre = UUID.randomUUID() + ".webp";
        Path destino = carpeta.resolve(nombre);

        try {
            Files.createDirectories(carpeta);
            escribirWebp(reducida, destino);
            return nombre;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(destino);
            } catch (IOException ignorada) {
                // el archivo parcial queda huerfano; no se pierde informacion
            }
            throw new AccesoDatosException(e);
        }
    }

    /** Lee una imagen ya guardada. Vacio si el nombre es nulo, invalido o el archivo no existe. */
    public Optional<BufferedImage> leer(String nombre) {

        Path ruta = resolver(nombre);
        if (ruta == null || !Files.isRegularFile(ruta)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(ImageIO.read(ruta.toFile()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /** Borra la imagen si existe; un fallo al borrar no interrumpe la operacion del usuario. */
    public void eliminar(String nombre) {

        Path ruta = resolver(nombre);
        if (ruta == null) {
            return;
        }
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException ignorada) {
            // queda un archivo huerfano en la carpeta de imagenes
        }
    }

    /** Solo acepta nombres generados por este servicio, evitando rutas manipuladas. */
    private Path resolver(String nombre) {

        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return null;
        }
        return carpeta.resolve(nombre);
    }

    private void validarOrigen(Path origen) {

        String archivo = origen.getFileName().toString().toLowerCase();
        int punto = archivo.lastIndexOf('.');
        String extension = punto < 0 ? "" : archivo.substring(punto + 1);

        if (!EXTENSIONES.contains(extension)) {
            throw new NegocioException("Formato no soportado. Usá PNG, JPG, GIF, BMP o WebP.");
        }
        try {
            if (!Files.isRegularFile(origen)) {
                throw new NegocioException("No se encontró el archivo de imagen.");
            }
            if (Files.size(origen) > MAX_BYTES_ORIGEN) {
                throw new NegocioException("La imagen supera los 20 MB.");
            }
        } catch (IOException e) {
            throw new NegocioException("No se pudo leer el archivo de imagen.");
        }
    }

    private BufferedImage leerOrigen(Path origen) {

        try (ImageInputStream entrada = ImageIO.createImageInputStream(origen.toFile())) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw new NegocioException("El archivo no es una imagen válida.");
            }

            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada);
                if ((long) lector.getWidth(0) * lector.getHeight(0) > MAX_PIXELES_ORIGEN) {
                    throw new NegocioException("La imagen tiene una resolución demasiado grande.");
                }
                return lector.read(0);
            } finally {
                lector.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof NegocioException negocio) {
                throw negocio;
            }
            throw new NegocioException("El archivo no es una imagen válida.");
        }
    }

    /** Redimensiona al maximo permitido y aplana la transparencia sobre fondo blanco. */
    private BufferedImage reducir(BufferedImage original) {

        int ancho = original.getWidth();
        int alto = original.getHeight();
        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(ancho, alto));
        int nuevoAncho = Math.max(1, (int) Math.round(ancho * escala));
        int nuevoAlto = Math.max(1, (int) Math.round(alto * escala));

        BufferedImage resultado = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resultado.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nuevoAncho, nuevoAlto);
            g.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
        } finally {
            g.dispose();
        }
        return resultado;
    }

    private void escribirWebp(BufferedImage imagen, Path destino) throws IOException {

        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!escritores.hasNext()) {
            throw new IOException("No hay un escritor WebP disponible.");
        }

        ImageWriter escritor = escritores.next();
        try (ImageOutputStream salida = ImageIO.createImageOutputStream(destino.toFile())) {
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            for (String tipo : parametros.getCompressionTypes()) {
                if (tipo.equalsIgnoreCase("Lossy")) {
                    parametros.setCompressionType(tipo);
                }
            }
            parametros.setCompressionQuality(CALIDAD);

            escritor.setOutput(salida);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } finally {
            escritor.dispose();
        }
    }
}

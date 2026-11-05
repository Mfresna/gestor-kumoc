package org.kumoc.service;

import org.kumoc.dao.ProductoDao;
import org.kumoc.exception.NegocioException;
import org.kumoc.model.Producto;

import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public class ProductoService {

    private final ProductoDao productoDao;
    private final ImagenService imagenService;

    public ProductoService(ProductoDao productoDao, ImagenService imagenService) {
        this.productoDao = productoDao;
        this.imagenService = imagenService;
    }

    public List<Producto> listar(boolean soloActivos) {
        return productoDao.listar(soloActivos);
    }

    public Optional<Producto> buscarPorId(int idProducto) {
        return productoDao.buscarPorId(idProducto);
    }

    /** Crea el producto si idProducto es null; si no, lo actualiza. Devuelve el producto guardado. */
    public Producto guardar(Producto producto) {

        String codigo = Validaciones.vacioANull(producto.codigo());
        String nombre = Validaciones.textoObligatorio(producto.nombre(), "El nombre es obligatorio.");

        if (producto.precioUnitario() == null || producto.precioUnitario().signum() < 0) {
            throw new NegocioException("El precio no puede ser negativo.");
        }
        if (producto.stockMinimo() < 0) {
            throw new NegocioException("El stock mínimo no puede ser negativo.");
        }

        if (codigo != null) {
            productoDao.buscarPorCodigo(codigo).ifPresent(existente -> {
                if (!existente.idProducto().equals(producto.idProducto())) {
                    throw new NegocioException("Ya existe un producto con ese código.");
                }
            });
        }

        Producto limpio = new Producto(producto.idProducto(), codigo, nombre,
                Validaciones.vacioANull(producto.talle()),
                Validaciones.vacioANull(producto.publicoObjetivo()),
                Validaciones.vacioANull(producto.color()),
                Validaciones.vacioANull(producto.descripcion()),
                producto.precioUnitario().setScale(2, RoundingMode.HALF_UP),
                producto.activo(), producto.stockMinimo(),
                Validaciones.vacioANull(producto.imagen()));

        if (limpio.idProducto() == null) {
            int id = productoDao.insertar(limpio);
            return new Producto(id, limpio.codigo(), limpio.nombre(), limpio.talle(), limpio.publicoObjetivo(),
                    limpio.color(), limpio.descripcion(), limpio.precioUnitario(), limpio.activo(),
                    limpio.stockMinimo(), limpio.imagen());
        }

        Producto anterior = productoDao.buscarPorId(limpio.idProducto())
                .orElseThrow(() -> new NegocioException("El producto a modificar no existe."));
        productoDao.actualizar(limpio);

        // La imagen reemplazada o quitada se borra recien con la actualizacion ya confirmada.
        if (anterior.imagen() != null && !anterior.imagen().equals(limpio.imagen())) {
            imagenService.eliminar(anterior.imagen());
        }
        return limpio;
    }

    /** Baja logica: el producto se conserva por el historial de movimientos y presupuestos. */
    public void darDeBaja(int idProducto) {
        productoDao.cambiarActivo(idProducto, false);
    }

    public void reactivar(int idProducto) {
        productoDao.cambiarActivo(idProducto, true);
    }
}

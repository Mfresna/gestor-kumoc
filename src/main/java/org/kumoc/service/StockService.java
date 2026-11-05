package org.kumoc.service;

import org.kumoc.dao.MovimientoStockDao;
import org.kumoc.dao.ProductoDao;
import org.kumoc.database.Transaccion;
import org.kumoc.exception.NegocioException;
import org.kumoc.model.AlertaStockBajo;
import org.kumoc.model.MovimientoStock;
import org.kumoc.model.Producto;
import org.kumoc.model.StockProducto;
import org.kumoc.model.TipoMovimiento;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registra movimientos y calcula el stock. Cada movimiento se valida y se inserta en una
 * transaccion; el stock fisico no puede quedar negativo.
 */
public class StockService {

    private final Connection connection;
    private final MovimientoStockDao movimientoDao;
    private final ProductoDao productoDao;

    public StockService(Connection connection, MovimientoStockDao movimientoDao, ProductoDao productoDao) {
        this.connection = connection;
        this.movimientoDao = movimientoDao;
        this.productoDao = productoDao;
    }

    public StockProducto obtenerStock(int idProducto) {
        return movimientoDao.obtenerStock(idProducto);
    }

    public Map<Integer, StockProducto> obtenerStockTodos() {
        return movimientoDao.obtenerStockTodos();
    }

    public List<MovimientoStock> historial(int idProducto) {
        return movimientoDao.listarPorProducto(idProducto);
    }

    public int registrarIngreso(int idProducto, int cantidad, String motivo, Integer idPresupuesto) {

        if (cantidad <= 0) {
            throw new NegocioException("La cantidad del ingreso debe ser mayor a cero.");
        }
        return registrar(idProducto, TipoMovimiento.INGRESO, cantidad, motivo, idPresupuesto);
    }

    public int registrarEgreso(int idProducto, int cantidad, String motivo, Integer idPresupuesto) {

        if (cantidad <= 0) {
            throw new NegocioException("La cantidad del egreso debe ser mayor a cero.");
        }
        return registrar(idProducto, TipoMovimiento.EGRESO, cantidad, motivo, idPresupuesto);
    }

    /** cantidad con signo: positiva suma, negativa resta. */
    public int registrarAjuste(int idProducto, int cantidad, String motivo) {

        if (cantidad == 0) {
            throw new NegocioException("El ajuste no puede ser cero.");
        }
        return registrar(idProducto, TipoMovimiento.AJUSTE, cantidad, motivo, null);
    }

    /**
     * Borra los movimientos ligados a una orden (fabricacion y venta) para que el stock vuelva a su valor previo.
     * Si algun producto quedara con stock fisico negativo lanza NegocioException; al correr dentro de la
     * transaccion de quien la llama, el rollback deja todo como estaba.
     */
    public void revertirMovimientosDeOrden(int idPresupuesto) {

        Transaccion.ejecutar(connection, () -> {
            Set<Integer> afectados = new LinkedHashSet<>();
            movimientoDao.listarPorPresupuesto(idPresupuesto).forEach(m -> afectados.add(m.idProducto()));

            movimientoDao.eliminarPorPresupuesto(idPresupuesto);

            for (int idProducto : afectados) {
                if (movimientoDao.obtenerStock(idProducto).fisico() < 0) {
                    String nombre = productoDao.buscarPorId(idProducto).map(Producto::nombre).orElse("un producto");
                    throw new NegocioException("No se puede eliminar la orden: el stock de \"" + nombre
                            + "\" quedaría en negativo (esas unidades ya se usaron en otra operación).");
                }
            }
        });
    }

    /** Productos activos con stock fisico calculado por debajo de su stock minimo. */
    public List<AlertaStockBajo> productosConStockBajo() {

        Map<Integer, StockProducto> stock = movimientoDao.obtenerStockTodos();
        List<AlertaStockBajo> alertas = new ArrayList<>();

        for (Producto producto : productoDao.listar(true)) {
            StockProducto s = stock.getOrDefault(producto.idProducto(), new StockProducto(producto.idProducto(), 0, 0));
            if (s.fisico() < producto.stockMinimo()) {
                alertas.add(new AlertaStockBajo(producto, s));
            }
        }
        return alertas;
    }

    private int registrar(int idProducto, TipoMovimiento tipo, int cantidad, String motivo, Integer idPresupuesto) {

        String motivoLimpio = Validaciones.textoObligatorio(motivo, "El motivo del movimiento es obligatorio.");

        return Transaccion.ejecutar(connection, () -> {
            if (productoDao.buscarPorId(idProducto).isEmpty()) {
                throw new NegocioException("El producto no existe.");
            }

            int efecto = tipo == TipoMovimiento.EGRESO ? -cantidad : cantidad;
            if (movimientoDao.obtenerStock(idProducto).fisico() + efecto < 0) {
                throw new NegocioException("Stock insuficiente: el movimiento dejaría el stock en negativo.");
            }

            return movimientoDao.insertar(new MovimientoStock(
                    null, idProducto, tipo, cantidad, LocalDateTime.now().withNano(0), motivoLimpio, idPresupuesto));
        });
    }
}

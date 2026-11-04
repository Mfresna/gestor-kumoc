package org.kumoc.service;

import org.kumoc.dao.ClienteDao;
import org.kumoc.dao.PresupuestoDao;
import org.kumoc.dao.ProductoDao;
import org.kumoc.database.Transaccion;
import org.kumoc.exception.NegocioException;
import org.kumoc.model.EstadoPresupuesto;
import org.kumoc.model.ItemPresupuesto;
import org.kumoc.model.Presupuesto;
import org.kumoc.model.Producto;

import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Ciclo presupuesto -> orden de trabajo -> venta. El pago y la facturacion en ARCA son
 * independientes del estado. Transiciones: PRESUPUESTO -> RECHAZADO | ORDEN_TRABAJO; ORDEN_TRABAJO -> VENTA.
 */
public class PresupuestoService {

    private final Connection connection;
    private final PresupuestoDao presupuestoDao;
    private final ClienteDao clienteDao;
    private final ProductoDao productoDao;
    private final StockService stockService;

    public PresupuestoService(Connection connection, PresupuestoDao presupuestoDao, ClienteDao clienteDao,
                              ProductoDao productoDao, StockService stockService) {
        this.connection = connection;
        this.presupuestoDao = presupuestoDao;
        this.clienteDao = clienteDao;
        this.productoDao = productoDao;
        this.stockService = stockService;
    }

    public Optional<Presupuesto> buscarPorId(int idPresupuesto) {
        return presupuestoDao.buscarPorId(idPresupuesto);
    }

    /** Todos los presupuestos, o solo los del estado indicado si no es null. */
    public List<Presupuesto> listar(EstadoPresupuesto estado) {
        return presupuestoDao.listar(estado);
    }

    /** Listado de fin de mes: cobrados con "Facturar en ARCA" que aun no fueron facturados. */
    public List<Presupuesto> listarPendientesFacturarArca() {
        return presupuestoDao.listarPendientesFacturarArca();
    }

    /**
     * Crea un presupuesto en estado PRESUPUESTO. Si el precio de un item es null se usa el
     * precio vigente del producto.
     */
    public Presupuesto crear(int idCliente, List<ItemPresupuesto> items) {

        return Transaccion.ejecutar(connection, () -> {
            validarCliente(idCliente);
            List<ItemPresupuesto> normalizados = normalizarItems(items);

            Presupuesto nuevo = new Presupuesto(null, idCliente, LocalDate.now(), EstadoPresupuesto.PRESUPUESTO,
                    normalizados, null, false, null);
            int id = presupuestoDao.insertar(nuevo);
            return obtener(id);
        });
    }

    /** Solo se puede modificar mientras esta en estado PRESUPUESTO. */
    public Presupuesto actualizar(int idPresupuesto, int idCliente, List<ItemPresupuesto> items) {

        return Transaccion.ejecutar(connection, () -> {
            Presupuesto actual = obtener(idPresupuesto);
            exigirEstado(actual, EstadoPresupuesto.PRESUPUESTO, "Solo se puede modificar un presupuesto.");
            validarCliente(idCliente);

            presupuestoDao.reemplazarContenido(new Presupuesto(idPresupuesto, idCliente, actual.fechaEmision(),
                    actual.estado(), normalizarItems(items), null, false, null));
            return obtener(idPresupuesto);
        });
    }

    public void rechazar(int idPresupuesto) {

        Transaccion.ejecutar(connection, () -> {
            exigirEstado(obtener(idPresupuesto), EstadoPresupuesto.PRESUPUESTO, "Solo se puede rechazar un presupuesto.");
            presupuestoDao.actualizarEstado(idPresupuesto, EstadoPresupuesto.RECHAZADO);
        });
    }

    /** Aprueba el presupuesto: pasa a orden de trabajo y su stock queda comprometido (congelado). */
    public void aprobarComoOrden(int idPresupuesto) {

        Transaccion.ejecutar(connection, () -> {
            exigirEstado(obtener(idPresupuesto), EstadoPresupuesto.PRESUPUESTO, "Solo se puede aprobar un presupuesto.");
            presupuestoDao.actualizarEstado(idPresupuesto, EstadoPresupuesto.ORDEN_TRABAJO);
        });
    }

    /** Unidades que faltan fabricar por producto (stock fisico menor al pedido). Vacio si no hay faltantes. */
    public Map<Integer, Integer> calcularFaltantes(int idPresupuesto) {

        Presupuesto presupuesto = obtener(idPresupuesto);
        Map<Integer, Integer> faltantes = new LinkedHashMap<>();

        for (ItemPresupuesto item : presupuesto.items()) {
            int fisico = stockService.obtenerStock(item.idProducto()).fisico();
            if (fisico < item.cantidad()) {
                faltantes.put(item.idProducto(), item.cantidad() - fisico);
            }
        }
        return faltantes;
    }

    /** Registra la fabricacion de unidades de una orden como INGRESO de stock con motivo "Fabricación". */
    public void registrarFabricacion(int idPresupuesto, int idProducto, int cantidad) {

        Transaccion.ejecutar(connection, () -> {
            Presupuesto presupuesto = obtener(idPresupuesto);
            exigirEstado(presupuesto, EstadoPresupuesto.ORDEN_TRABAJO, "Solo se registra fabricación en una orden de trabajo.");

            if (presupuesto.items().stream().noneMatch(i -> i.idProducto() == idProducto)) {
                throw new NegocioException("El producto no pertenece a la orden.");
            }
            stockService.registrarIngreso(idProducto, cantidad, "Fabricación", idPresupuesto);
        });
    }

    /**
     * Pasa la orden a venta registrada: exige stock fisico suficiente para cada item y genera
     * un EGRESO por item, todo en una transaccion.
     */
    public void pasarAVenta(int idPresupuesto) {

        Transaccion.ejecutar(connection, () -> {
            Presupuesto presupuesto = obtener(idPresupuesto);
            exigirEstado(presupuesto, EstadoPresupuesto.ORDEN_TRABAJO, "Solo una orden de trabajo puede pasar a venta.");

            if (!calcularFaltantes(idPresupuesto).isEmpty()) {
                throw new NegocioException("Aún hay faltantes por fabricar; no se puede registrar la venta.");
            }

            for (ItemPresupuesto item : presupuesto.items()) {
                stockService.registrarEgreso(item.idProducto(), item.cantidad(), "Venta", idPresupuesto);
            }
            presupuestoDao.actualizarEstado(idPresupuesto, EstadoPresupuesto.VENTA);
        });
    }

    /** Registra el cobro (realizado fuera de la aplicacion) y si queda marcada para facturar en ARCA. */
    public void registrarCobro(int idPresupuesto, LocalDate fechaCobro, boolean facturarArca) {

        if (fechaCobro == null) {
            throw new NegocioException("La fecha de cobro es obligatoria.");
        }

        Transaccion.ejecutar(connection, () -> {
            Presupuesto presupuesto = obtener(idPresupuesto);

            if (presupuesto.estado() != EstadoPresupuesto.ORDEN_TRABAJO && presupuesto.estado() != EstadoPresupuesto.VENTA) {
                throw new NegocioException("Solo se registra el cobro de una orden de trabajo o una venta.");
            }
            if (presupuesto.cobrado()) {
                throw new NegocioException("El cobro ya fue registrado.");
            }
            presupuestoDao.registrarCobro(idPresupuesto, fechaCobro, facturarArca);
        });
    }

    /** Marca como facturada en ARCA una orden cobrada con el flag activo; la saca del listado de fin de mes. */
    public void marcarFacturadoArca(int idPresupuesto, LocalDate fecha) {

        if (fecha == null) {
            throw new NegocioException("La fecha de facturación es obligatoria.");
        }

        Transaccion.ejecutar(connection, () -> {
            Presupuesto presupuesto = obtener(idPresupuesto);

            if (!presupuesto.cobrado() || !presupuesto.facturarArca()) {
                throw new NegocioException("La orden no está cobrada o no está marcada para facturar en ARCA.");
            }
            if (presupuesto.fechaFacturacionArca() != null) {
                throw new NegocioException("La orden ya fue facturada en ARCA.");
            }
            presupuestoDao.registrarFacturacionArca(idPresupuesto, fecha);
        });
    }

    /** Solo se puede eliminar un presupuesto o uno rechazado; las ordenes y ventas quedan como registro. */
    public void eliminar(int idPresupuesto) {

        Transaccion.ejecutar(connection, () -> {
            Presupuesto presupuesto = obtener(idPresupuesto);

            if (presupuesto.estado() != EstadoPresupuesto.PRESUPUESTO && presupuesto.estado() != EstadoPresupuesto.RECHAZADO) {
                throw new NegocioException("No se puede eliminar una orden de trabajo ni una venta.");
            }
            presupuestoDao.eliminar(idPresupuesto);
        });
    }

    private Presupuesto obtener(int idPresupuesto) {
        return presupuestoDao.buscarPorId(idPresupuesto)
                .orElseThrow(() -> new NegocioException("El presupuesto no existe."));
    }

    private void exigirEstado(Presupuesto presupuesto, EstadoPresupuesto esperado, String mensaje) {

        if (presupuesto.estado() != esperado) {
            throw new NegocioException(mensaje + " Estado actual: " + presupuesto.estado() + ".");
        }
    }

    private void validarCliente(int idCliente) {

        if (clienteDao.buscarPorId(idCliente).isEmpty()) {
            throw new NegocioException("El cliente no existe.");
        }
    }

    private List<ItemPresupuesto> normalizarItems(List<ItemPresupuesto> items) {

        if (items == null || items.isEmpty()) {
            throw new NegocioException("El presupuesto debe tener al menos un producto.");
        }

        Set<Integer> vistos = new HashSet<>();
        List<ItemPresupuesto> normalizados = new ArrayList<>();

        for (ItemPresupuesto item : items) {
            if (!vistos.add(item.idProducto())) {
                throw new NegocioException("Hay un producto repetido en el presupuesto.");
            }
            if (item.cantidad() <= 0) {
                throw new NegocioException("La cantidad de cada producto debe ser mayor a cero.");
            }

            Producto producto = productoDao.buscarPorId(item.idProducto())
                    .orElseThrow(() -> new NegocioException("Un producto del presupuesto no existe."));
            if (!producto.activo()) {
                throw new NegocioException("El producto \"" + producto.nombre() + "\" está dado de baja.");
            }

            var precio = (item.precioUnitario() != null ? item.precioUnitario() : producto.precioUnitario())
                    .setScale(2, RoundingMode.HALF_UP);
            if (precio.signum() <= 0) {
                throw new NegocioException("El precio de \"" + producto.nombre() + "\" debe ser mayor a cero.");
            }

            normalizados.add(new ItemPresupuesto(null, item.idProducto(), item.cantidad(), precio));
        }
        return normalizados;
    }
}

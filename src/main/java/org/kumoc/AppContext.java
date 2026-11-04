package org.kumoc;

import org.kumoc.config.AppPaths;
import org.kumoc.dao.ClienteDao;
import org.kumoc.dao.MovimientoStockDao;
import org.kumoc.dao.PresupuestoDao;
import org.kumoc.dao.ProductoDao;
import org.kumoc.database.DatabaseManager;
import org.kumoc.service.ClienteService;
import org.kumoc.service.ImagenService;
import org.kumoc.service.PresupuestoService;
import org.kumoc.service.ProductoService;
import org.kumoc.service.StockService;

import java.nio.file.Path;
import java.sql.Connection;

/**
 * Arma una sola vez la conexion, los DAOs y los servicios (aplicacion monousuario).
 * Los controladores obtienen los servicios desde aqui.
 */
public final class AppContext {

    private static AppContext instancia;

    private final Connection connection;
    private final ClienteService clienteService;
    private final ImagenService imagenService;
    private final ProductoService productoService;
    private final StockService stockService;
    private final PresupuestoService presupuestoService;

    private AppContext(Connection connection, Path carpetaImagenes) {

        this.connection = connection;
        this.imagenService = new ImagenService(carpetaImagenes);

        ClienteDao clienteDao = new ClienteDao(connection);
        ProductoDao productoDao = new ProductoDao(connection);
        MovimientoStockDao movimientoDao = new MovimientoStockDao(connection);
        PresupuestoDao presupuestoDao = new PresupuestoDao(connection);

        this.clienteService = new ClienteService(clienteDao);
        this.productoService = new ProductoService(productoDao, imagenService);
        this.stockService = new StockService(connection, movimientoDao, productoDao);
        this.presupuestoService = new PresupuestoService(connection, presupuestoDao, clienteDao, productoDao, stockService);
    }

    public static void inicializar() throws Exception {
        instancia = new AppContext(DatabaseManager.initialize(), AppPaths.getImagesFolder());
    }

    /** Variante con conexion y carpeta de imagenes propias (pruebas con base temporal). */
    public static void inicializar(Connection connection, Path carpetaImagenes) {
        instancia = new AppContext(connection, carpetaImagenes);
    }

    public static AppContext get() {

        if (instancia == null) {
            throw new IllegalStateException("AppContext no fue inicializado.");
        }
        return instancia;
    }

    public static void cerrar() throws Exception {

        if (instancia != null) {
            instancia.connection.close();
            instancia = null;
        }
    }

    public ClienteService clientes() {
        return clienteService;
    }

    public ImagenService imagenes() {
        return imagenService;
    }

    public ProductoService productos() {
        return productoService;
    }

    public StockService stock() {
        return stockService;
    }

    public PresupuestoService presupuestos() {
        return presupuestoService;
    }
}

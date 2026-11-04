package org.kumoc.dao;

import org.kumoc.exception.AccesoDatosException;
import org.kumoc.model.EstadoPresupuesto;
import org.kumoc.model.ItemPresupuesto;
import org.kumoc.model.Presupuesto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PresupuestoDao {

    private record Cabecera(int id, int idCliente, LocalDate fechaEmision, EstadoPresupuesto estado,
                            LocalDate fechaCobro, boolean facturarArca, LocalDate fechaFacturacionArca) {
    }

    private final Connection connection;

    public PresupuestoDao(Connection connection) {
        this.connection = connection;
    }

    /** Inserta cabecera e items y devuelve el id generado. Conviene llamarlo dentro de una transaccion. */
    public int insertar(Presupuesto p) {

        String sql = "INSERT INTO PRESUPUESTO(id_cliente, fecha_emision, estado, total, fecha_cobro, "
                + "facturar_arca, fecha_facturacion_arca) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.idCliente());
            ps.setString(2, p.fechaEmision().toString());
            ps.setString(3, p.estado().name());
            ps.setDouble(4, p.total().doubleValue());
            SqlUtil.setFecha(ps, 5, p.fechaCobro());
            ps.setInt(6, p.facturarArca() ? 1 : 0);
            SqlUtil.setFecha(ps, 7, p.fechaFacturacionArca());
            ps.executeUpdate();

            int id;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
            }
            insertarItems(id, p.items());
            return id;
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    /** Reemplaza cliente, total e items. Conviene llamarlo dentro de una transaccion. */
    public void reemplazarContenido(Presupuesto p) {

        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE PRESUPUESTO SET id_cliente = ?, total = ? WHERE id_presupuesto = ?")) {
                ps.setInt(1, p.idCliente());
                ps.setDouble(2, p.total().doubleValue());
                ps.setInt(3, p.idPresupuesto());
                ps.executeUpdate();
            }
            borrarItems(p.idPresupuesto());
            insertarItems(p.idPresupuesto(), p.items());
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public void actualizarEstado(int idPresupuesto, EstadoPresupuesto estado) {

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE PRESUPUESTO SET estado = ? WHERE id_presupuesto = ?")) {
            ps.setString(1, estado.name());
            ps.setInt(2, idPresupuesto);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public void registrarCobro(int idPresupuesto, LocalDate fechaCobro, boolean facturarArca) {

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE PRESUPUESTO SET fecha_cobro = ?, facturar_arca = ? WHERE id_presupuesto = ?")) {
            ps.setString(1, fechaCobro.toString());
            ps.setInt(2, facturarArca ? 1 : 0);
            ps.setInt(3, idPresupuesto);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public void registrarFacturacionArca(int idPresupuesto, LocalDate fecha) {

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE PRESUPUESTO SET fecha_facturacion_arca = ? WHERE id_presupuesto = ?")) {
            ps.setString(1, fecha.toString());
            ps.setInt(2, idPresupuesto);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    /** Elimina cabecera e items. Conviene llamarlo dentro de una transaccion. */
    public void eliminar(int idPresupuesto) {

        try {
            borrarItems(idPresupuesto);
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM PRESUPUESTO WHERE id_presupuesto = ?")) {
                ps.setInt(1, idPresupuesto);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public Optional<Presupuesto> buscarPorId(int idPresupuesto) {
        return consultar("WHERE p.id_presupuesto = ?", idPresupuesto).stream().findFirst();
    }

    /** Lista todos los presupuestos, o solo los del estado indicado si no es null. */
    public List<Presupuesto> listar(EstadoPresupuesto estado) {
        return estado == null
                ? consultar("")
                : consultar("WHERE p.estado = ?", estado.name());
    }

    /** Cobrados con flag "Facturar en ARCA" que aun no fueron facturados (listado de fin de mes). */
    public List<Presupuesto> listarPendientesFacturarArca() {
        return consultar("WHERE p.fecha_cobro IS NOT NULL AND p.facturar_arca = 1 "
                + "AND p.fecha_facturacion_arca IS NULL");
    }

    /** El filtro es siempre una constante del codigo; los valores van como parametros. */
    private List<Presupuesto> consultar(String where, Object... parametros) {

        String sqlCabecera = "SELECT p.id_presupuesto, p.id_cliente, p.fecha_emision, p.estado, p.fecha_cobro, "
                + "p.facturar_arca, p.fecha_facturacion_arca FROM PRESUPUESTO p " + where
                + " ORDER BY p.fecha_emision DESC, p.id_presupuesto DESC";

        String sqlItems = "SELECT i.id_prodxpres, i.id_presupuesto, i.id_producto, i.cantidad, i.precio_unitario "
                + "FROM PRODUCTOSxPRESUPUESTOS i JOIN PRESUPUESTO p ON p.id_presupuesto = i.id_presupuesto "
                + where + " ORDER BY i.id_prodxpres";

        try {
            List<Cabecera> cabeceras = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(sqlCabecera)) {
                asignarParametros(ps, parametros);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        cabeceras.add(new Cabecera(
                                rs.getInt("id_presupuesto"),
                                rs.getInt("id_cliente"),
                                SqlUtil.aFecha(rs.getString("fecha_emision")),
                                EstadoPresupuesto.valueOf(rs.getString("estado")),
                                SqlUtil.aFecha(rs.getString("fecha_cobro")),
                                rs.getInt("facturar_arca") == 1,
                                SqlUtil.aFecha(rs.getString("fecha_facturacion_arca"))));
                    }
                }
            }

            // Items de todos los presupuestos filtrados en una sola consulta (evita N+1).
            Map<Integer, List<ItemPresupuesto>> itemsPorPresupuesto = new HashMap<>();
            try (PreparedStatement ps = connection.prepareStatement(sqlItems)) {
                asignarParametros(ps, parametros);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        itemsPorPresupuesto
                                .computeIfAbsent(rs.getInt("id_presupuesto"), k -> new ArrayList<>())
                                .add(new ItemPresupuesto(
                                        rs.getInt("id_prodxpres"),
                                        rs.getInt("id_producto"),
                                        rs.getInt("cantidad"),
                                        SqlUtil.aDinero(rs.getDouble("precio_unitario"))));
                    }
                }
            }

            List<Presupuesto> presupuestos = new ArrayList<>();
            for (Cabecera c : cabeceras) {
                presupuestos.add(new Presupuesto(
                        c.id(), c.idCliente(), c.fechaEmision(), c.estado(),
                        itemsPorPresupuesto.getOrDefault(c.id(), List.of()),
                        c.fechaCobro(), c.facturarArca(), c.fechaFacturacionArca()));
            }
            return presupuestos;
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    private void asignarParametros(PreparedStatement ps, Object[] parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }
    }

    private void insertarItems(int idPresupuesto, List<ItemPresupuesto> items) throws SQLException {

        String sql = "INSERT INTO PRODUCTOSxPRESUPUESTOS(id_presupuesto, id_producto, cantidad, precio_unitario, subtotal) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (ItemPresupuesto item : items) {
                ps.setInt(1, idPresupuesto);
                ps.setInt(2, item.idProducto());
                ps.setInt(3, item.cantidad());
                ps.setDouble(4, item.precioUnitario().doubleValue());
                ps.setDouble(5, item.subtotal().doubleValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void borrarItems(int idPresupuesto) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM PRODUCTOSxPRESUPUESTOS WHERE id_presupuesto = ?")) {
            ps.setInt(1, idPresupuesto);
            ps.executeUpdate();
        }
    }
}

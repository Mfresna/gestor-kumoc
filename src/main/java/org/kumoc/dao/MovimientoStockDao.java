package org.kumoc.dao;

import org.kumoc.exception.AccesoDatosException;
import org.kumoc.model.MovimientoStock;
import org.kumoc.model.StockProducto;
import org.kumoc.model.TipoMovimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Historico de movimientos y calculo de stock. El stock nunca se guarda: siempre se deriva
 * de MOVIMIENTO_STOCK (fisico) y de los items de presupuestos en ORDEN_TRABAJO (comprometido).
 */
public class MovimientoStockDao {

    private static final String SELECT_STOCK = """
            SELECT p.id_producto,
                   COALESCE((SELECT SUM(CASE m.tipo_movimiento WHEN 'EGRESO' THEN -m.cantidad ELSE m.cantidad END)
                             FROM MOVIMIENTO_STOCK m
                             WHERE m.id_producto = p.id_producto), 0) AS fisico,
                   COALESCE((SELECT SUM(i.cantidad)
                             FROM PRODUCTOSxPRESUPUESTOS i
                             JOIN PRESUPUESTO pr ON pr.id_presupuesto = i.id_presupuesto
                             WHERE i.id_producto = p.id_producto
                               AND pr.estado = 'ORDEN_TRABAJO'), 0) AS comprometido
            FROM PRODUCTOS p
            """;

    private final Connection connection;

    public MovimientoStockDao(Connection connection) {
        this.connection = connection;
    }

    /** Inserta el movimiento (fecha incluida) y devuelve su id generado. */
    public int insertar(MovimientoStock m) {

        String sql = "INSERT INTO MOVIMIENTO_STOCK(id_producto, tipo_movimiento, cantidad, fecha, motivo, id_presupuesto) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.idProducto());
            ps.setString(2, m.tipo().name());
            ps.setInt(3, m.cantidad());
            ps.setString(4, m.fecha().format(SqlUtil.FECHA_HORA));
            ps.setString(5, m.motivo());
            SqlUtil.setEntero(ps, 6, m.idPresupuesto());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public List<MovimientoStock> listarPorProducto(int idProducto) {
        return listar("WHERE id_producto = ?", idProducto);
    }

    public List<MovimientoStock> listarPorPresupuesto(int idPresupuesto) {
        return listar("WHERE id_presupuesto = ?", idPresupuesto);
    }

    /** Borra los movimientos ligados a una orden (fabricacion y venta). Conviene llamarlo dentro de una transaccion. */
    public void eliminarPorPresupuesto(int idPresupuesto) {

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM MOVIMIENTO_STOCK WHERE id_presupuesto = ?")) {
            ps.setInt(1, idPresupuesto);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public StockProducto obtenerStock(int idProducto) {

        try (PreparedStatement ps = connection.prepareStatement(SELECT_STOCK + "WHERE p.id_producto = ?")) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearStock(rs) : new StockProducto(idProducto, 0, 0);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    /** Stock de todos los productos en una sola consulta, indexado por id de producto. */
    public Map<Integer, StockProducto> obtenerStockTodos() {

        try (PreparedStatement ps = connection.prepareStatement(SELECT_STOCK);
             ResultSet rs = ps.executeQuery()) {

            Map<Integer, StockProducto> stock = new LinkedHashMap<>();
            while (rs.next()) {
                StockProducto s = mapearStock(rs);
                stock.put(s.idProducto(), s);
            }
            return stock;
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    private List<MovimientoStock> listar(String where, int parametro) {

        String sql = "SELECT id_movimiento, id_producto, tipo_movimiento, cantidad, fecha, motivo, id_presupuesto "
                + "FROM MOVIMIENTO_STOCK " + where + " ORDER BY fecha DESC, id_movimiento DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                List<MovimientoStock> movimientos = new ArrayList<>();
                while (rs.next()) {
                    // wasNull() refiere a la ultima columna leida, asi que se consulta justo despues de leerla.
                    int idPresupuesto = rs.getInt("id_presupuesto");
                    boolean sinPresupuesto = rs.wasNull();
                    movimientos.add(new MovimientoStock(
                            rs.getInt("id_movimiento"),
                            rs.getInt("id_producto"),
                            TipoMovimiento.valueOf(rs.getString("tipo_movimiento")),
                            rs.getInt("cantidad"),
                            SqlUtil.aFechaHora(rs.getString("fecha")),
                            rs.getString("motivo"),
                            sinPresupuesto ? null : idPresupuesto));
                }
                return movimientos;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    private StockProducto mapearStock(ResultSet rs) throws SQLException {
        return new StockProducto(
                rs.getInt("id_producto"),
                rs.getInt("fisico"),
                rs.getInt("comprometido"));
    }
}

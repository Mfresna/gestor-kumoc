package org.kumoc.dao;

import org.kumoc.exception.AccesoDatosException;
import org.kumoc.model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoDao {

    private static final String COLUMNAS = "id_producto, codigo, nombre, talle, publico_objetivo, color, "
            + "descripcion, precio_unitario, activo, stock_minimo, imagen";

    private final Connection connection;

    public ProductoDao(Connection connection) {
        this.connection = connection;
    }

    /** Inserta el producto y devuelve su id generado. */
    public int insertar(Producto p) {

        String sql = "INSERT INTO PRODUCTOS(codigo, nombre, talle, publico_objetivo, color, descripcion, "
                + "precio_unitario, activo, stock_minimo, imagen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignar(ps, p);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public void actualizar(Producto p) {

        String sql = "UPDATE PRODUCTOS SET codigo = ?, nombre = ?, talle = ?, publico_objetivo = ?, color = ?, "
                + "descripcion = ?, precio_unitario = ?, activo = ?, stock_minimo = ?, imagen = ?, "
                + "ult_actualizacion = datetime('now') WHERE id_producto = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            asignar(ps, p);
            ps.setInt(11, p.idProducto());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public void cambiarActivo(int idProducto, boolean activo) {

        String sql = "UPDATE PRODUCTOS SET activo = ?, ult_actualizacion = datetime('now') WHERE id_producto = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setInt(2, idProducto);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public Optional<Producto> buscarPorId(int idProducto) {

        String sql = "SELECT " + COLUMNAS + " FROM PRODUCTOS WHERE id_producto = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public Optional<Producto> buscarPorCodigo(String codigo) {

        String sql = "SELECT " + COLUMNAS + " FROM PRODUCTOS WHERE codigo = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public List<Producto> listar(boolean soloActivos) {

        String sql = "SELECT " + COLUMNAS + " FROM PRODUCTOS"
                + (soloActivos ? " WHERE activo = 1" : "") + " ORDER BY nombre, talle";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Producto> productos = new ArrayList<>();
            while (rs.next()) {
                productos.add(mapear(rs));
            }
            return productos;
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    private void asignar(PreparedStatement ps, Producto p) throws SQLException {
        ps.setString(1, p.codigo());
        ps.setString(2, p.nombre());
        ps.setString(3, p.talle());
        ps.setString(4, p.publicoObjetivo());
        ps.setString(5, p.color());
        ps.setString(6, p.descripcion());
        ps.setDouble(7, p.precioUnitario().doubleValue());
        ps.setInt(8, p.activo() ? 1 : 0);
        ps.setInt(9, p.stockMinimo());
        ps.setString(10, p.imagen());
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getInt("id_producto"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("talle"),
                rs.getString("publico_objetivo"),
                rs.getString("color"),
                rs.getString("descripcion"),
                SqlUtil.aDinero(rs.getDouble("precio_unitario")),
                rs.getInt("activo") == 1,
                rs.getInt("stock_minimo"),
                rs.getString("imagen"));
    }
}

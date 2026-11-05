package org.kumoc.dao;

import org.kumoc.exception.AccesoDatosException;
import org.kumoc.model.Cliente;
import org.kumoc.model.TipoFactura;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteDao {

    private static final String COLUMNAS =
            "id_cliente, razon_social, cuit, tipo_factura, email, telefono, direccion, "
                    + "localidad, codigo_postal, provincia, activo";

    private final Connection connection;

    public ClienteDao(Connection connection) {
        this.connection = connection;
    }

    /** Inserta el cliente y devuelve su id generado. */
    public int insertar(Cliente c) {

        String sql = "INSERT INTO CLIENTES(razon_social, cuit, tipo_factura, email, telefono, direccion, "
                + "localidad, codigo_postal, provincia) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.razonSocial());
            ps.setString(2, c.cuit());
            ps.setString(3, c.tipoFactura().name());
            ps.setString(4, c.email());
            ps.setString(5, c.telefono());
            ps.setString(6, c.direccion());
            ps.setString(7, c.localidad());
            ps.setString(8, c.codigoPostal());
            ps.setString(9, c.provincia());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public void actualizar(Cliente c) {

        String sql = "UPDATE CLIENTES SET razon_social = ?, cuit = ?, tipo_factura = ?, email = ?, "
                + "telefono = ?, direccion = ?, localidad = ?, codigo_postal = ?, provincia = ? WHERE id_cliente = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.razonSocial());
            ps.setString(2, c.cuit());
            ps.setString(3, c.tipoFactura().name());
            ps.setString(4, c.email());
            ps.setString(5, c.telefono());
            ps.setString(6, c.direccion());
            ps.setString(7, c.localidad());
            ps.setString(8, c.codigoPostal());
            ps.setString(9, c.provincia());
            ps.setInt(10, c.idCliente());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public void cambiarActivo(int idCliente, boolean activo) {

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE CLIENTES SET activo = ? WHERE id_cliente = ?")) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setInt(2, idCliente);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public Optional<Cliente> buscarPorId(int idCliente) {

        String sql = "SELECT " + COLUMNAS + " FROM CLIENTES WHERE id_cliente = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public Optional<Cliente> buscarPorCuit(String cuit) {

        String sql = "SELECT " + COLUMNAS + " FROM CLIENTES WHERE cuit = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cuit);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public List<Cliente> listar(boolean soloActivos) {

        String sql = "SELECT " + COLUMNAS + " FROM CLIENTES "
                + (soloActivos ? "WHERE activo = 1 " : "") + "ORDER BY razon_social";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Cliente> clientes = new ArrayList<>();
            while (rs.next()) {
                clientes.add(mapear(rs));
            }
            return clientes;
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id_cliente"),
                rs.getString("razon_social"),
                rs.getString("cuit"),
                TipoFactura.valueOf(rs.getString("tipo_factura")),
                rs.getString("email"),
                rs.getString("telefono"),
                rs.getString("direccion"),
                rs.getString("localidad"),
                rs.getString("codigo_postal"),
                rs.getString("provincia"),
                rs.getInt("activo") == 1);
    }
}

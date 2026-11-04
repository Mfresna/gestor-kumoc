package org.kumoc.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Conversiones entre tipos Java y el almacenamiento TEXT/REAL de SQLite. */
final class SqlUtil {

    static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SqlUtil() {
    }

    static LocalDate aFecha(String valor) {
        return valor == null ? null : LocalDate.parse(valor);
    }

    static LocalDateTime aFechaHora(String valor) {
        return valor == null ? null : LocalDateTime.parse(valor, FECHA_HORA);
    }

    static BigDecimal aDinero(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    static void setFecha(PreparedStatement ps, int indice, LocalDate fecha) throws SQLException {
        if (fecha == null) {
            ps.setNull(indice, Types.VARCHAR);
        } else {
            ps.setString(indice, fecha.toString());
        }
    }

    static void setEntero(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, valor);
        }
    }
}

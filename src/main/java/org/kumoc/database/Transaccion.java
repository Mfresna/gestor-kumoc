package org.kumoc.database;

import org.kumoc.exception.AccesoDatosException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/** Ejecuta una operacion en una transaccion; si ya hay una abierta, se une a ella. */
public final class Transaccion {

    private Transaccion() {
    }

    public static <T> T ejecutar(Connection connection, Supplier<T> operacion) {

        try {
            if (!connection.getAutoCommit()) {
                return operacion.get();
            }

            connection.setAutoCommit(false);
            try {
                T resultado = operacion.get();
                connection.commit();
                return resultado;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(e);
        }
    }

    public static void ejecutar(Connection connection, Runnable operacion) {
        ejecutar(connection, () -> {
            operacion.run();
            return null;
        });
    }
}

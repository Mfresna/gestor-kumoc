package org.kumoc.database;

import org.kumoc.config.AppPaths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionFactory {

    public static Connection getConnection() throws SQLException {

        String url = "jdbc:sqlite:" + AppPaths.getDatabasePath();
        Connection  connection = DriverManager.getConnection(url);


        //Sentencia para control las FK en cada conexion
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }

        return connection;
    }
}
package org.kumoc.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {

    public static Connection getConnection() throws SQLException {

        String url = "jdbc:sqlite:"
                + AppPaths.getDatabasePath();

        return DriverManager.getConnection(url);
    }
}
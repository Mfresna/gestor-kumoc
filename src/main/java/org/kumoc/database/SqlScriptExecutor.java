package org.kumoc.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlScriptExecutor {

    public static void execute(Connection connection, String resourcePath) throws IOException, SQLException {

        InputStream inputStream = SqlScriptExecutor.class.getResourceAsStream(resourcePath);
        String sql;

        if (inputStream == null) {
            throw new IOException("No se encontró el recurso SQL: " + resourcePath);
        }

        try (inputStream) {
            sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (Statement statement = connection.createStatement()) {

            //Ejecuta el .sql
            statement.execute(sql);
        }
    }
}
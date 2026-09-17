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
            //Convierte el SQL en STRING
            sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        //Arreglo de Sentencias a Ejecutar
        String[] sentencias = sql.split(";");

        try (Statement statement = connection.createStatement()) {

            for (String sentencia : sentencias) {

                sentencia = sentencia.trim();

                if (!sentencia.isEmpty()) {

                    logExecute(sentencia);

                    statement.execute(sentencia);
                }
            }
        }
    }

    private static void logExecute(String sentencia) {

        System.out.println("Ejecutando: " +
                sentencia.substring(0, Math.min(50, sentencia.length())));
    }
}

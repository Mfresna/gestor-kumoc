package org.kumoc.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {

    public static void execute(Connection connection)
            throws IOException, SQLException {

        InputStream inputStream =
                SchemaInitializer.class
                        .getResourceAsStream("/db/schema.sql");

        if (inputStream == null) {
            throw new RuntimeException(
                    "No se encontró el archivo schema.sql"
            );
        }

        String sql = new String(
                inputStream.readAllBytes(),
                StandardCharsets.UTF_8
        );

        System.out.println("Schema encontrado:");
        System.out.println(sql);

        try (Statement statement =
                     connection.createStatement()) {

            statement.execute(sql);
        }

        System.out.println("Schema ejecutado correctamente.");
    }
}
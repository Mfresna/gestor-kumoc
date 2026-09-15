package org.kumoc.database;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    public static Connection initialize()
            throws IOException, SQLException {

        // 1. Crear carpeta Kumoc
        Files.createDirectories(
                AppPaths.getAppFolder()
        );

        // 2. Crear carpeta imagenes
        Files.createDirectories(
                AppPaths.getImagesFolder()
        );

        // 3. Verificar si la base ya existía
        boolean databaseExists =
                Files.exists(
                        AppPaths.getDatabasePath()
                );

        // 4. Abrir conexión
        // Si el archivo no existe, SQLite lo crea.
        Connection connection =
                ConnectionFactory.getConnection();

        // 5. Si era una BD nueva, ejecutar schema.sql
        if (!databaseExists) {

            System.out.println(
                    "Base de datos nueva. Ejecutando schema..."
            );

            SchemaInitializer.execute(connection);

            System.out.println(
                    "Base de datos creada correctamente."
            );

        } else {

            System.out.println(
                    "Base de datos existente."
            );
        }

        // 6. Devolver la conexión
        return connection;
    }
}
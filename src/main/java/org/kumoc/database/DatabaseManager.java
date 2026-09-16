package org.kumoc.database;

import org.kumoc.config.AppPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    public static Connection initialize() throws IOException, SQLException {

        //Crea los Paths
        AppPaths.crearDirectorios();


        boolean databaseExists = Files.exists(AppPaths.getDatabasePath());

        //Si el archivo no existe lo crea pero vacio, por eso el if siguiente
        Connection connection = ConnectionFactory.getConnection();

        if (!databaseExists) {
            System.out.println("Base de datos nueva. Ejecutando SQL...");

            CrearDataBase.execute(connection);

        } else {

            System.out.println("Base de datos existente.");
        }

        return connection;
    }
}
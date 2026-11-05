package org.kumoc.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPaths {

    private static final String APP_NAME = "Kumoc";

    public static void crearDirectorios() throws IOException {
        Files.createDirectories(AppPaths.getAppFolder());
        Files.createDirectories(AppPaths.getImagesFolder());
    }

    public static Path getAppFolder() {

        String appData = System.getenv("APPDATA");
        return Paths.get(appData, APP_NAME);
    }

    public static Path getDatabasePath() {
        //Crea si no existe o devuelve la ruta si lo enucentra
        return getAppFolder().resolve("kumoc.db");
    }

    /** Datos del negocio (contacto e informacion de pago) que se imprimen en el pie de los PDF. */
    public static Path getBusinessDataPath() {

        return getAppFolder().resolve("negocio.properties");
    }

    public static Path getImagesFolder() {

        return getAppFolder().resolve("imagenes");
    }

}
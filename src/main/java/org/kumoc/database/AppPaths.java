package org.kumoc.database;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPaths {

    private static final String APP_NAME = "Kumoc";

    public static Path getAppFolder() {

        String appData = System.getenv("APPDATA");
        return Paths.get(appData, APP_NAME);
    }

    public static Path getDatabasePath() {

        return getAppFolder().resolve("kumoc.db");
    }

    public static Path getImagesFolder() {

        return getAppFolder().resolve("imagenes");
    }
}
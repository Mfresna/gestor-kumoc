package org.kumoc;

import javafx.application.Application;

/** Punto de entrada. Se separa de App para poder ejecutar JavaFX desde el classpath. */
public class Main {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}

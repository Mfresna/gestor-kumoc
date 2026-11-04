package org.kumoc.ui;

import javafx.scene.control.TableView;

/** Ajustes comunes de tablas. */
public final class Tablas {

    private Tablas() {
    }

    /** Hace que las columnas se repartan el ancho disponible en lugar de mostrar scroll horizontal. */
    public static void ajustarColumnas(TableView<?>... tablas) {

        for (TableView<?> tabla : tablas) {
            tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }
    }
}

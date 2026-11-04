package org.kumoc.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.kumoc.AppContext;

/** Estado observable compartido entre secciones (hoy: cantidad de alertas de stock bajo). */
public final class EstadoApp {

    private static final IntegerProperty ALERTAS_STOCK = new SimpleIntegerProperty(0);

    private EstadoApp() {
    }

    public static IntegerProperty alertasStock() {
        return ALERTAS_STOCK;
    }

    /** Recalcula las alertas de stock bajo; llamar despues de cualquier cambio de stock o de productos. */
    public static void refrescarAlertas() {
        ALERTAS_STOCK.set(AppContext.get().stock().productosConStockBajo().size());
    }
}

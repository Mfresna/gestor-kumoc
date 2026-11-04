package org.kumoc.model;

/** Stock calculado (nunca almacenado) de un producto. */
public record StockProducto(
        int idProducto,
        int fisico,
        int comprometido
) {

    public int disponible() {
        return fisico - comprometido;
    }
}

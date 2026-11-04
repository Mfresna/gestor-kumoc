package org.kumoc.model;

/** Producto activo cuyo stock fisico calculado esta por debajo de su stock minimo. */
public record AlertaStockBajo(
        Producto producto,
        StockProducto stock
) {
}

package org.kumoc.model;

import java.math.BigDecimal;

/** idProducto es null mientras el producto no fue guardado. El stock no es parte del producto: se deriva de los movimientos. */
public record Producto(
        Integer idProducto,
        String codigo,
        String nombre,
        String talle,
        String publicoObjetivo,
        String color,
        String descripcion,
        BigDecimal precioUnitario,
        boolean activo,
        int stockMinimo,
        String imagen
) {
}

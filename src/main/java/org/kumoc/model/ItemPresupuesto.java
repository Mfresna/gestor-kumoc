package org.kumoc.model;

import java.math.BigDecimal;

public record ItemPresupuesto(
        Integer idItem,
        int idProducto,
        int cantidad,
        BigDecimal precioUnitario
) {

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}

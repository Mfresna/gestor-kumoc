package org.kumoc.model;

import java.time.LocalDateTime;

/**
 * Evento inmutable de inventario. INGRESO/EGRESO llevan cantidad positiva; AJUSTE lleva signo.
 * idPresupuesto vincula el movimiento con la orden/venta que lo origino (puede ser null).
 */
public record MovimientoStock(
        Integer idMovimiento,
        int idProducto,
        TipoMovimiento tipo,
        int cantidad,
        LocalDateTime fecha,
        String motivo,
        Integer idPresupuesto
) {

    /** Efecto del movimiento sobre el stock fisico. */
    public int efecto() {
        return tipo == TipoMovimiento.EGRESO ? -cantidad : cantidad;
    }
}

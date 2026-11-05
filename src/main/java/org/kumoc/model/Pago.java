package org.kumoc.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un cobro (total o parcial) de una orden. idPago es null mientras no fue guardado. */
public record Pago(
        Integer idPago,
        int idPresupuesto,
        LocalDate fecha,
        BigDecimal monto
) {
}

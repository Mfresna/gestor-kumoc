package org.kumoc.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cubre todo el ciclo presupuesto -> orden de trabajo -> venta.
 * El pago (fechaCobro) y la facturacion en ARCA son ejes independientes del estado.
 */
public record Presupuesto(
        Integer idPresupuesto,
        int idCliente,
        LocalDate fechaEmision,
        EstadoPresupuesto estado,
        List<ItemPresupuesto> items,
        LocalDate fechaCobro,
        boolean facturarArca,
        LocalDate fechaFacturacionArca
) {

    public BigDecimal total() {
        return items.stream()
                .map(ItemPresupuesto::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean cobrado() {
        return fechaCobro != null;
    }
}

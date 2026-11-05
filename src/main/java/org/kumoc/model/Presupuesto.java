package org.kumoc.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Cubre todo el ciclo presupuesto -> orden de trabajo -> venta.
 * El pago (uno o varios pagos parciales) y la facturacion en ARCA son ejes independientes del estado.
 */
public record Presupuesto(
        Integer idPresupuesto,
        int idCliente,
        LocalDate fechaEmision,
        EstadoPresupuesto estado,
        List<ItemPresupuesto> items,
        List<Pago> pagos,
        boolean facturarArca,
        LocalDate fechaFacturacionArca
) {

    public BigDecimal total() {
        return items.stream()
                .map(ItemPresupuesto::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPagado() {
        return pagos.stream()
                .map(Pago::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Lo que falta cobrar; nunca negativo. */
    public BigDecimal saldo() {
        return total().subtract(totalPagado()).max(BigDecimal.ZERO);
    }

    /** Cobrado por completo: tiene pagos y no queda saldo. */
    public boolean cobrado() {
        return !pagos.isEmpty() && saldo().signum() == 0;
    }

    /** Hay al menos un pago pero todavia queda saldo. */
    public boolean cobroParcial() {
        return !pagos.isEmpty() && saldo().signum() > 0;
    }

    /** Fecha del pago que completo el cobro; null mientras haya saldo. */
    public LocalDate fechaCobro() {
        return cobrado()
                ? pagos.stream().map(Pago::fecha).max(Comparator.naturalOrder()).orElse(null)
                : null;
    }
}

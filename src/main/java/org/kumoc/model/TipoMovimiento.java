package org.kumoc.model;

public enum TipoMovimiento {
    INGRESO,
    EGRESO,
    /** Correccion manual; la cantidad lleva signo. */
    AJUSTE
}

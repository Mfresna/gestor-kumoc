package org.kumoc.model;

import java.util.List;

/**
 * Lo que falta fabricar de un producto considerando todas las ordenes en produccion:
 * aFabricar = max(0, pedido - fisico).
 */
public record FaltanteFabricacion(
        Producto producto,
        int pedido,
        int fisico,
        int aFabricar,
        List<Integer> ordenes
) {
}

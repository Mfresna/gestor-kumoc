package org.kumoc.model;

/** idCliente es null mientras el cliente no fue guardado. */
public record Cliente(
        Integer idCliente,
        String razonSocial,
        String cuit,
        TipoFactura tipoFactura,
        String email,
        String telefono,
        String direccion
) {
}

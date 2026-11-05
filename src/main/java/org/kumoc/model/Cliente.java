package org.kumoc.model;

/** idCliente es null mientras el cliente no fue guardado. cuit, localidad, codigoPostal y provincia son null si no se informaron. */
public record Cliente(
        Integer idCliente,
        String razonSocial,
        String cuit,
        TipoFactura tipoFactura,
        String email,
        String telefono,
        String direccion,
        String localidad,
        String codigoPostal,
        String provincia,
        boolean activo
) {
}

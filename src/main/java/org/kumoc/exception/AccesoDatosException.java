package org.kumoc.exception;

/**
 * Falla de acceso a datos. El mensaje es generico (apto para el usuario);
 * el detalle tecnico queda solo en la causa, para el log.
 */
public class AccesoDatosException extends RuntimeException {

    public AccesoDatosException(Throwable causa) {
        super("Error al acceder a la base de datos.", causa);
    }
}

package org.kumoc.exception;

/** Error de regla de negocio o validacion; su mensaje es apto para mostrarse al usuario. */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}

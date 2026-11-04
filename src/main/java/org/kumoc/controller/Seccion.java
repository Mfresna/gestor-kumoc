package org.kumoc.controller;

/** Controlador de una seccion de la ventana principal. */
public interface Seccion {

    /** Se llama cada vez que la seccion se muestra; sirve para recargar sus datos. */
    void alMostrar();

    /** Se llama antes de reemplazar la seccion por otra; sirve para descartar trabajo no guardado. */
    default void alSalir() {
    }
}

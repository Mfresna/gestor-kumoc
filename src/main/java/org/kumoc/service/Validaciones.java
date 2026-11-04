package org.kumoc.service;

import org.kumoc.exception.NegocioException;

final class Validaciones {

    private Validaciones() {
    }

    static String textoObligatorio(String valor, String mensaje) {

        if (valor == null || valor.isBlank()) {
            throw new NegocioException(mensaje);
        }
        return valor.trim();
    }

    static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}

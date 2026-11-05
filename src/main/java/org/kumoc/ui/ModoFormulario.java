package org.kumoc.ui;

/** Estado de un formulario de alta/edicion: define el texto del boton principal y si los campos se editan. */
public enum ModoFormulario {

    /** Registro nuevo, todavia sin guardar. */
    NUEVO("GUARDAR NUEVO", true),
    /** Registro existente seleccionado, solo lectura. */
    VISTA("EDITAR", false),
    /** Registro existente con los campos habilitados. */
    EDICION("GUARDAR", true);

    private final String textoBoton;
    private final boolean camposEditables;

    ModoFormulario(String textoBoton, boolean camposEditables) {
        this.textoBoton = textoBoton;
        this.camposEditables = camposEditables;
    }

    public String textoBoton() {
        return textoBoton;
    }

    public boolean camposEditables() {
        return camposEditables;
    }
}

package org.borradoruno.shared.network;

public class Mensaje {
    private String tipo;
    private Object datos;

    public Mensaje(String tipo, Object datos) {
        this.tipo = tipo;
        this.datos = datos;
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Object getDatos() { return datos; }
    public void setDatos(Object datos) { this.datos = datos; }
}
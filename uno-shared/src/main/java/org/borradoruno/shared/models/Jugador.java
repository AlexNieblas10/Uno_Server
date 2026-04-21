package org.borradoruno.shared.models;

import java.util.ArrayList;
import java.util.List;

public class Jugador {
    private String nombre;
    private String idSesion;
    private boolean esAnfitrion;
    private boolean dijoUNO;
    private int puntos;
    private List<Carta> mano;

    public Jugador(String nombre, String idSesion) {
        this.nombre = nombre;
        this.idSesion = idSesion;
        this.esAnfitrion = false;
        this.dijoUNO = false;
        this.puntos = 0;
        this.mano = new ArrayList<>();
    }

    // Getters and Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getIdSesion() { return idSesion; }
    public void setIdSesion(String idSesion) { this.idSesion = idSesion; }
    public boolean isEsAnfitrion() { return esAnfitrion; }
    public void setEsAnfitrion(boolean esAnfitrion) { this.esAnfitrion = esAnfitrion; }
    public boolean isDijoUNO() { return dijoUNO; }
    public void setDijoUNO(boolean dijoUNO) { this.dijoUNO = dijoUNO; }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
    public List<Carta> getMano() { return mano; }
    public void setMano(List<Carta> mano) { this.mano = mano; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Jugador jugador = (Jugador) o;
        return java.util.Objects.equals(nombre, jugador.nombre);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(nombre);
    }
}
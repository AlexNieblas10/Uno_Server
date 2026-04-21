package org.borradoruno.shared.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mazo {
    private List<Carta> cartas;

    public Mazo() {
        this.cartas = new ArrayList<>();
    }

    public int getCartasRestantes() {
        return cartas.size();
    }

    public void barajar() {
        Collections.shuffle(cartas);
    }

    public Carta robar() {
        if (cartas.isEmpty()) return null;
        return cartas.remove(cartas.size() - 1);
    }

    public List<Carta> getCartas() { return cartas; }
    public void setCartas(List<Carta> cartas) { this.cartas = cartas; }
}
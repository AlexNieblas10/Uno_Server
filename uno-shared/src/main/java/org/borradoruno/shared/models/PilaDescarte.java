package org.borradoruno.shared.models;

import java.util.ArrayList;
import java.util.List;

public class PilaDescarte {
    private Color colorActivo;
    private Valor valorActivo;
    private List<Carta> cartas;

    public PilaDescarte() {
        this.cartas = new ArrayList<>();
    }

    public void agregarCarta(Carta carta) {
        this.cartas.add(carta);
        this.colorActivo = carta.getColor();
        this.valorActivo = carta.getValor();
    }

    // Getters and Setters
    public Color getColorActivo() { return colorActivo; }
    public void setColorActivo(Color colorActivo) { this.colorActivo = colorActivo; }
    public Valor getValorActivo() { return valorActivo; }
    public void setValorActivo(Valor valorActivo) { this.valorActivo = valorActivo; }
    public List<Carta> getCartas() { return cartas; }
}
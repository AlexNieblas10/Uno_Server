package org.borradoruno.shared.models;

public class Carta {
    private Color color;
    private Valor valor;
    private boolean esEspecial;
    private int puntos;

    public Carta(Color color, Valor valor, boolean esEspecial, int puntos) {
        this.color = color;
        this.valor = valor;
        this.esEspecial = esEspecial;
        this.puntos = puntos;
    }

    // Getters and Setters
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public Valor getValor() { return valor; }
    public void setValor(Valor valor) { this.valor = valor; }
    public boolean isEsEspecial() { return esEspecial; }
    public void setEsEspecial(boolean esEspecial) { this.esEspecial = esEspecial; }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
}
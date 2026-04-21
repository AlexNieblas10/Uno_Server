package org.borradoruno.shared.models;

import java.util.ArrayList;
import java.util.List;

public class Partida {
    private String idPartida;
    private String codigoSala;
    private int maxJugadores;
    private Sentido sentidoJuego;
    private int turnoActual;
    private EstadoPartida estado;
    private int cartasAComer;
    private List<Jugador> jugadores;
    private Mazo mazo;
    private PilaDescarte pilaDescarte;

    public Partida(String idPartida) {
        this.idPartida = idPartida;
        this.codigoSala = "UNO-" + (int)(Math.random() * 9000 + 1000);
        this.maxJugadores = 4;
        this.sentidoJuego = Sentido.HORARIO;
        this.turnoActual = 0;
        this.estado = EstadoPartida.ESPERANDO_JUGADORES;
        this.cartasAComer = 0;
        this.jugadores = new ArrayList<>();
        this.mazo = new Mazo();
        this.pilaDescarte = new PilaDescarte();
    }

    // Getters and Setters
    public String getCodigoSala() { return codigoSala; }
    public void setCodigoSala(String codigoSala) { this.codigoSala = codigoSala; }
    public int getMaxJugadores() { return maxJugadores; }
    public void setMaxJugadores(int maxJugadores) { this.maxJugadores = maxJugadores; }
    public String getIdPartida() { return idPartida; }
    public void setIdPartida(String idPartida) { this.idPartida = idPartida; }
    public Sentido getSentidoJuego() { return sentidoJuego; }
    public void setSentidoJuego(Sentido sentidoJuego) { this.sentidoJuego = sentidoJuego; }
    public int getTurnoActual() { return turnoActual; }
    public void setTurnoActual(int turnoActual) { this.turnoActual = turnoActual; }
    public EstadoPartida getEstado() { return estado; }
    public void setEstado(EstadoPartida estado) { this.estado = estado; }
    public int getCartasAComer() { return cartasAComer; }
    public void setCartasAComer(int cartasAComer) { this.cartasAComer = cartasAComer; }
    public List<Jugador> getJugadores() { return jugadores; }
    public void setJugadores(List<Jugador> jugadores) { this.jugadores = jugadores; }
    public Mazo getMazo() { return mazo; }
    public void setMazo(Mazo mazo) { this.mazo = mazo; }
    public PilaDescarte getPilaDescarte() { return pilaDescarte; }
    public void setPilaDescarte(PilaDescarte pilaDescarte) { this.pilaDescarte = pilaDescarte; }
}
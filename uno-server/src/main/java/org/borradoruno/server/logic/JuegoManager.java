package org.borradoruno.server.logic;

import org.borradoruno.shared.models.Partida;
import org.borradoruno.shared.models.Jugador;
import java.util.UUID;

public class JuegoManager {
    private static JuegoManager instance;
    private Partida partidaActual;

    private JuegoManager() {
        this.partidaActual = new Partida(UUID.randomUUID().toString());
    }

    public static synchronized JuegoManager getInstance() {
        if (instance == null) {
            instance = new JuegoManager();
        }
        return instance;
    }

    public void inicializarMazo() {
        org.borradoruno.shared.models.Mazo mazo = partidaActual.getMazo();
        mazo.getCartas().clear();
        for (org.borradoruno.shared.models.Color c : org.borradoruno.shared.models.Color.values()) {
            if (c == org.borradoruno.shared.models.Color.NEGRO) continue;
            for (org.borradoruno.shared.models.Valor v : org.borradoruno.shared.models.Valor.values()) {
                if (v == org.borradoruno.shared.models.Valor.COMODIN_COLOR || v == org.borradoruno.shared.models.Valor.COMODIN_MAS_CUATRO) continue;
                // UNO tiene dos de cada especial y un 0
                mazo.getCartas().add(new org.borradoruno.shared.models.Carta(c, v, false, 0));
                if (v != org.borradoruno.shared.models.Valor.CERO) {
                    mazo.getCartas().add(new org.borradoruno.shared.models.Carta(c, v, false, 0));
                }
            }
        }
        // Comodines
        for (int i = 0; i < 4; i++) {
            mazo.getCartas().add(new org.borradoruno.shared.models.Carta(org.borradoruno.shared.models.Color.NEGRO, org.borradoruno.shared.models.Valor.COMODIN_COLOR, true, 50));
            mazo.getCartas().add(new org.borradoruno.shared.models.Carta(org.borradoruno.shared.models.Color.NEGRO, org.borradoruno.shared.models.Valor.COMODIN_MAS_CUATRO, true, 50));
        }
        mazo.barajar();
    }

    public synchronized void iniciarPartida() {
        if (partidaActual.getJugadores().size() < 2) return;
        
        inicializarMazo();
        partidaActual.setEstado(org.borradoruno.shared.models.EstadoPartida.EN_CURSO);
        
        // Repartir 7 cartas a cada uno
        for (Jugador j : partidaActual.getJugadores()) {
            for (int i = 0; i < 7; i++) {
                j.getMano().add(partidaActual.getMazo().robar());
            }
        }
        
        // Primera carta a la pila
        partidaActual.getPilaDescarte().agregarCarta(partidaActual.getMazo().robar());

        // Iniciar el timer del primer turno
        partidaActual.setTurnoIniciadoEn(System.currentTimeMillis());
    }

    public synchronized boolean validarJugada(Jugador jugador, org.borradoruno.shared.models.Carta carta) {
        // 1. Validar que sea el turno del jugador
        int indiceJugador = partidaActual.getJugadores().indexOf(jugador);
        if (indiceJugador != partidaActual.getTurnoActual()) {
            System.out.println("Jugada rechazada: No es el turno de " + jugador.getNombre());
            return false;
        }

        org.borradoruno.shared.models.PilaDescarte pila = partidaActual.getPilaDescarte();
        // 2. Si es comodín negro, es válido
        if (carta.getColor() == org.borradoruno.shared.models.Color.NEGRO) return true;
        
        // 3. Si la carta en la pila es negra (comodín recién tirado), permitir cualquier carta
        if (pila.getColorActivo() == org.borradoruno.shared.models.Color.NEGRO) return true;

        // 4. Si coincide color o valor
        boolean coincide = carta.getColor() == pila.getColorActivo() || carta.getValor() == pila.getValorActivo();
        if (!coincide) System.out.println("Jugada rechazada: La carta " + carta.getValor() + " no coincide con la pila");
        return coincide;
    }

    public synchronized boolean procesarJugada(Jugador jugador, org.borradoruno.shared.models.Carta carta) {
        if (!validarJugada(jugador, carta)) return false;

        // Quitar solo la PRIMERA carta coincidente (removeIf eliminaría duplicados)
        int idx = -1;
        java.util.List<org.borradoruno.shared.models.Carta> mano = jugador.getMano();
        for (int i = 0; i < mano.size(); i++) {
            org.borradoruno.shared.models.Carta c = mano.get(i);
            if (c.getColor() == carta.getColor() && c.getValor() == carta.getValor()) {
                idx = i; break;
            }
        }
        if (idx == -1) {
            System.err.println("Error: " + jugador.getNombre() + " intentó tirar una carta que no tiene: " + carta.getValor());
            return false;
        }
        jugador.getMano().remove(idx);

        partidaActual.getPilaDescarte().agregarCarta(carta);

        if (carta.getColor() == org.borradoruno.shared.models.Color.NEGRO) {
            partidaActual.getPilaDescarte().setColorActivo(org.borradoruno.shared.models.Color.ROJO);
        }

        aplicarEfectos(carta);
        verificarGanador(jugador);
        avanzarTurno();
        System.out.println("Jugada exitosa de " + jugador.getNombre() + ". Siguiente turno: " + partidaActual.getTurnoActual());
        return true;
    }

    public synchronized boolean procesarJugadaComodin(Jugador jugador, org.borradoruno.shared.models.Carta comodin, org.borradoruno.shared.models.Color colorElegido) {
        if (!validarJugada(jugador, comodin)) return false;

        int idx = -1;
        java.util.List<org.borradoruno.shared.models.Carta> mano = jugador.getMano();
        for (int i = 0; i < mano.size(); i++) {
            org.borradoruno.shared.models.Carta c = mano.get(i);
            if (c.getColor() == comodin.getColor() && c.getValor() == comodin.getValor()) {
                idx = i; break;
            }
        }
        if (idx == -1) {
            System.err.println("Error: " + jugador.getNombre() + " intentó tirar un comodín que no tiene: " + comodin.getValor());
            return false;
        }
        jugador.getMano().remove(idx);

        partidaActual.getPilaDescarte().agregarCarta(comodin);
        partidaActual.getPilaDescarte().setColorActivo(colorElegido);

        aplicarEfectos(comodin);
        verificarGanador(jugador);
        avanzarTurno();
        System.out.println("Jugada exitosa de " + jugador.getNombre() + " con comodín. Color elegido: " + colorElegido + ". Siguiente turno: " + partidaActual.getTurnoActual());
        return true;
    }

    private void aplicarEfectos(org.borradoruno.shared.models.Carta carta) {
        switch (carta.getValor()) {
            case REVERSA:
                partidaActual.setSentidoJuego(
                    partidaActual.getSentidoJuego() == org.borradoruno.shared.models.Sentido.HORARIO ?
                    org.borradoruno.shared.models.Sentido.ANTIHORARIO : org.borradoruno.shared.models.Sentido.HORARIO
                );
                break;
            case MAS_DOS:
                // Castigo automático: el siguiente jugador roba 2 y pierde su turno
                avanzarTurno();
                Jugador victimaDos = partidaActual.getJugadores().get(partidaActual.getTurnoActual());
                System.out.println(victimaDos.getNombre() + " roba 2 automáticamente y salta su turno");
                for (int i = 0; i < 2; i++) {
                    if (partidaActual.getMazo().getCartasRestantes() == 0) reciclarMazo();
                    victimaDos.getMano().add(partidaActual.getMazo().robar());
                }
                break;
            case BLOQUEO:
                // Saltamos un turno extra antes de avanzar el normal
                avanzarTurno();
                break;
            case COMODIN_MAS_CUATRO:
                // Castigo automático: el siguiente jugador roba 4 y pierde su turno
                avanzarTurno();
                Jugador victimaCuatro = partidaActual.getJugadores().get(partidaActual.getTurnoActual());
                System.out.println(victimaCuatro.getNombre() + " roba 4 automáticamente y salta su turno");
                for (int i = 0; i < 4; i++) {
                    if (partidaActual.getMazo().getCartasRestantes() == 0) reciclarMazo();
                    victimaCuatro.getMano().add(partidaActual.getMazo().robar());
                }
                break;
            case COMODIN_COLOR:
                break;
        }
    }

    private void avanzarTurno() {
        int total = partidaActual.getJugadores().size();
        int paso = partidaActual.getSentidoJuego() == org.borradoruno.shared.models.Sentido.HORARIO ? 1 : -1;
        partidaActual.setTurnoActual((partidaActual.getTurnoActual() + paso + total) % total);
        partidaActual.setTurnoIniciadoEn(System.currentTimeMillis());
    }

    public synchronized boolean robarCarta(Jugador jugador) {
        int indiceJugador = partidaActual.getJugadores().indexOf(jugador);
        if (indiceJugador != partidaActual.getTurnoActual()) {
            System.out.println("robarCarta rechazada: No es el turno de " + jugador.getNombre());
            return false;
        }
        if (partidaActual.getMazo().getCartasRestantes() == 0) reciclarMazo();
        jugador.getMano().add(partidaActual.getMazo().robar());
        jugador.setDijoUNO(false); // ya no tiene 1 carta, pierde la protección
        avanzarTurno();
        return true;
    }

    public synchronized boolean atraparUno(String nombreVictima) {
        Jugador victima = partidaActual.getJugadores().stream()
                .filter(j -> j.getNombre().equals(nombreVictima))
                .findFirst().orElse(null);

        if (victima == null) return false;

        // Solo se puede atrapar si tiene exactamente 1 carta Y no dijo UNO
        if (victima.getMano().size() != 1 || victima.isDijoUNO()) {
            System.out.println("Intento de atrapar fallido: " + nombreVictima + " ya dijo UNO o no tiene 1 carta");
            return false;
        }

        // Penalización: roba 2 cartas
        System.out.println("¡" + nombreVictima + " fue atrapado sin decir UNO! Roba 2 cartas de penalización.");
        for (int i = 0; i < 2; i++) {
            if (partidaActual.getMazo().getCartasRestantes() == 0) reciclarMazo();
            victima.getMano().add(partidaActual.getMazo().robar());
        }
        victima.setDijoUNO(false);
        return true;
    }

    private void reciclarMazo() {
        // Lógica para pasar cartas de PilaDescarte a Mazo (excepto la última)
        org.borradoruno.shared.models.PilaDescarte pila = partidaActual.getPilaDescarte();
        java.util.List<org.borradoruno.shared.models.Carta> viejas = pila.getCartas();
        org.borradoruno.shared.models.Carta actual = viejas.remove(viejas.size() - 1);
        
        partidaActual.getMazo().getCartas().addAll(viejas);
        partidaActual.getMazo().barajar();
        viejas.clear();
        viejas.add(actual);
    }

    public Partida getPartidaActual() {
        return partidaActual;
    }

    public synchronized void agregarJugador(Jugador jugador) {
        if (partidaActual.getJugadores().size() == 0) {
            jugador.setEsAnfitrion(true);
        }
        partidaActual.getJugadores().add(jugador);
    }

    public synchronized void setMaxJugadores(int max) {
        this.partidaActual.setMaxJugadores(max);
    }

    public synchronized void removerJugador(Jugador jugador) {
        boolean eraAnfitrion = jugador.isEsAnfitrion();
        partidaActual.getJugadores().remove(jugador);

        // Reasignar Anfitrión si se fue el actual (Diagrama 3)
        if (eraAnfitrion && !partidaActual.getJugadores().isEmpty()) {
            partidaActual.getJugadores().get(0).setEsAnfitrion(true);
        }
    }

    public synchronized void reiniciarPartida() {
        // Conservar jugadores y configuración de sala
        java.util.List<Jugador> jugadores = new java.util.ArrayList<>(partidaActual.getJugadores());
        String codigoSala = partidaActual.getCodigoSala();
        int maxJugadores = partidaActual.getMaxJugadores();

        this.partidaActual = new Partida(UUID.randomUUID().toString());
        this.partidaActual.setCodigoSala(codigoSala);
        this.partidaActual.setMaxJugadores(maxJugadores);

        for (Jugador j : jugadores) {
            j.getMano().clear();
            j.setDijoUNO(false);
            j.setListo(false);
            this.partidaActual.getJugadores().add(j);
        }
        System.out.println("Partida reiniciada. Jugadores: " + jugadores.size());
    }

    public synchronized void marcarListo(Jugador jugador) {
        jugador.setListo(true);
        System.out.println(jugador.getNombre() + " está listo");
    }

    public synchronized void marcarUno(Jugador jugador) {
        if (jugador.getMano().size() == 1) {
            jugador.setDijoUNO(true);
            System.out.println(jugador.getNombre() + " marcó UNO correctamente (tiene 1 carta)");
        } else {
            System.out.println(jugador.getNombre() + " dijo UNO pero tiene " + jugador.getMano().size() + " cartas");
        }
    }

    private void verificarGanador(Jugador jugador) {
        if (jugador.getMano().isEmpty()) {
            System.out.println("¡" + jugador.getNombre() + " ha ganado la partida!");
            partidaActual.setEstado(org.borradoruno.shared.models.EstadoPartida.FINALIZADA);
        }
    }

    // Aquí se implementarán más métodos de lógica según los diagramas de flujo
}
package org.borradoruno.server.validation;

import org.borradoruno.shared.models.*;

public class GameStateValidator {

    /**
     * Valida que una carta existe en la mano del jugador.
     * Busca por color y valor.
     */
    public static ValidationResult validateCardInHand(Jugador jugador, Carta carta) {
        if (jugador == null) {
            return ValidationResult.failure("JUGADOR_NULL", "El jugador no puede ser null");
        }

        if (carta == null) {
            return ValidationResult.failure("CARTA_NULL", "La carta no puede ser null");
        }

        boolean found = jugador.getMano().stream()
            .anyMatch(c -> c.getColor() == carta.getColor() && c.getValor() == carta.getValor());

        if (!found) {
            return ValidationResult.failure("CARD_NOT_IN_HAND",
                "El jugador no tiene la carta " + carta.getValor() + " " + carta.getColor() + " en su mano");
        }

        return ValidationResult.success();
    }

    /**
     * Valida que un índice de jugador esté dentro de los límites.
     * Previene ArrayIndexOutOfBoundsException.
     */
    public static ValidationResult validatePlayerIndex(Partida partida, int index) {
        if (partida == null || partida.getJugadores() == null) {
            return ValidationResult.failure("PARTIDA_NULL", "La partida no puede ser null");
        }

        if (index < 0 || index >= partida.getJugadores().size()) {
            return ValidationResult.failure("INDEX_OUT_OF_BOUNDS",
                "Índice de jugador fuera de límites: " + index +
                " (total jugadores: " + partida.getJugadores().size() + ")");
        }

        return ValidationResult.success();
    }

    /**
     * Valida que la partida esté en el estado correcto antes de procesar acciones.
     */
    public static ValidationResult validateGameState(Partida partida, EstadoPartida requiredState) {
        if (partida == null) {
            return ValidationResult.failure("PARTIDA_NULL", "La partida no puede ser null");
        }

        if (partida.getEstado() != requiredState) {
            return ValidationResult.failure("INVALID_STATE",
                "La partida debe estar en estado " + requiredState +
                " (estado actual: " + partida.getEstado() + ")");
        }

        return ValidationResult.success();
    }

    /**
     * Valida que una jugada sea permitida según las reglas del juego.
     * Verifica color o valor coincidente con la pila de descarte.
     */
    public static ValidationResult validateCardPlay(Carta carta, PilaDescarte pila) {
        if (carta == null) {
            return ValidationResult.failure("CARTA_NULL", "La carta no puede ser null");
        }

        if (pila == null) {
            return ValidationResult.failure("PILA_NULL", "La pila de descarte no puede ser null");
        }

        // Comodines siempre son válidos
        if (carta.getColor() == Color.NEGRO) {
            return ValidationResult.success();
        }

        // Si la pila tiene un comodín, cualquier carta es válida
        if (pila.getColorActivo() == Color.NEGRO) {
            return ValidationResult.success();
        }

        // Verificar coincidencia de color o valor
        boolean coincideColor = carta.getColor() == pila.getColorActivo();
        boolean coincideValor = carta.getValor() == pila.getValorActivo();

        if (!coincideColor && !coincideValor) {
            return ValidationResult.failure("INVALID_CARD_PLAY",
                "La carta " + carta.getValor() + " " + carta.getColor() +
                " no coincide con la pila (color: " + pila.getColorActivo() +
                ", valor: " + pila.getValorActivo() + ")");
        }

        return ValidationResult.success();
    }

    /**
     * Valida que haya suficientes jugadores para iniciar la partida.
     */
    public static ValidationResult validateMinPlayers(Partida partida, int minPlayers) {
        if (partida == null || partida.getJugadores() == null) {
            return ValidationResult.failure("PARTIDA_NULL", "La partida no puede ser null");
        }

        if (partida.getJugadores().size() < minPlayers) {
            return ValidationResult.failure("NOT_ENOUGH_PLAYERS",
                "Se necesitan al menos " + minPlayers + " jugadores para iniciar " +
                "(jugadores actuales: " + partida.getJugadores().size() + ")");
        }

        return ValidationResult.success();
    }
}

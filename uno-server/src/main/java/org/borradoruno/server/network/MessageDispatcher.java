package org.borradoruno.server.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.borradoruno.server.logic.JuegoManager;
import org.borradoruno.server.validation.InputValidator;
import org.borradoruno.server.validation.ValidationResult;
import org.borradoruno.shared.models.*;
import org.borradoruno.shared.network.Mensaje;

/**
 * Despacha mensajes recibidos del cliente hacia la lógica de negocio correcta.
 * Solo extrae datos del mensaje, valida y delega — no maneja sockets directamente.
 */
public class MessageDispatcher {

    private final Gson gson;
    private final Server server;

    public MessageDispatcher(Server server) {
        this.server = server;
        this.gson = new Gson();
    }

    /**
     * Enruta el mensaje al manejador correspondiente.
     *
     * @param mensaje      Mensaje ya parseado desde el JSON recibido
     * @param jugador      Referencia mutable al jugador de esta conexión (puede actualizarse)
     * @param sesionId     Identificador de sesión (dirección del socket)
     * @param portador     Objeto que encapsula la referencia al jugador para poder actualizarla
     * @return JSON de respuesta, o null si se hizo broadcast desde aquí
     */
    public String despachar(Mensaje mensaje, JugadorPortador portador, String sesionId) {
        try {
            switch (mensaje.getTipo()) {
                case "CREATE":
                case "JOIN":
                    return manejarRegistro(mensaje, portador, sesionId);

                case "SET_MAX_JUGADORES":
                    return manejarMaxJugadores(mensaje, portador.get());

                case "INICIAR_PARTIDA":
                    JuegoManager.getInstance().iniciarPartida();
                    return broadcast();

                case "TIRAR_CARTA":
                    return manejarTirarCarta(mensaje, portador.get());

                case "TIRAR_COMODIN":
                    return manejarComodin(mensaje, portador.get());

                case "ROBAR_CARTA":
                    if (JuegoManager.getInstance().robarCarta(portador.get())) {
                        return broadcast();
                    }
                    return error("No es tu turno para robar");

                case "REINICIAR_PARTIDA":
                    if (portador.get() != null && portador.get().isEsAnfitrion()) {
                        JuegoManager.getInstance().reiniciarPartida();
                        return broadcast();
                    }
                    return null;

                case "MARCAR_LISTO":
                    if (portador.get() != null) {
                        JuegoManager.getInstance().marcarListo(portador.get());
                        return broadcast();
                    }
                    return null;

                case "DECIR_UNO":
                    JuegoManager.getInstance().marcarUno(portador.get());
                    System.out.println(portador.get().getNombre() + " dijo UNO!");
                    return broadcast();

                case "ABANDONAR_SALA":
                    JuegoManager.getInstance().removerJugador(portador.get());
                    return broadcast();

                case "ATRAPAR_UNO":
                    return manejarAtraparUno(mensaje);

                case "SOLICITAR_ESTADO":
                    return gson.toJson(new Mensaje("ESTADO_PARTIDA",
                            JuegoManager.getInstance().getPartidaActual()));

                case "HANDSHAKE":
                case "PING":
                    return null; // ignorar silenciosamente

                default:
                    return error("Tipo de mensaje desconocido: " + mensaje.getTipo());
            }
        } catch (Exception e) {
            System.err.println("Error despachando mensaje: " + e.getMessage());
            e.printStackTrace();
            return error("Error interno del servidor");
        }
    }

    // -------------------------------------------------------------------------
    // Manejadores privados
    // -------------------------------------------------------------------------

    private String manejarRegistro(Mensaje mensaje, JugadorPortador portador, String sesionId) {
        if (mensaje.getDatos() == null) {
            return error("El nombre no puede ser null");
        }

        String nombre;
        String avatarStr = "AZUL";
        String codigoSala = null;

        if (mensaje.getDatos() instanceof java.util.List) {
            java.util.List<?> lista = (java.util.List<?>) mensaje.getDatos();
            nombre = lista.get(0).toString();
            if (mensaje.getTipo().equals("CREATE")) {
                // [nombre, avatar]
                if (lista.size() > 1) avatarStr = lista.get(1).toString();
            } else {
                // JOIN: [nombre, codigo, avatar]
                if (lista.size() > 1) codigoSala = lista.get(1).toString();
                if (lista.size() > 2) avatarStr = lista.get(2).toString();
            }
        } else {
            nombre = mensaje.getDatos().toString();
        }

        // Validación de formato
        ValidationResult nicknameResult = InputValidator.validateNickname(nombre);
        if (!nicknameResult.isValid()) {
            return error(nicknameResult.getErrorMessage());
        }

        // Validación de unicidad (delegada a JuegoManager)
        if (JuegoManager.getInstance().verificarNicknameUnico(nombre)) {
            return error("El apodo '" + nombre + "' ya está en uso");
        }

        // Registro y creación del jugador (delegado a JuegoManager)
        Jugador jugador = JuegoManager.getInstance()
                .prepararYRegistrarJugador(nombre, avatarStr, sesionId,
                        mensaje.getTipo().equals("CREATE"));

        if (jugador == null) {
            return error("La sala está llena");
        }

        portador.set(jugador);
        return broadcast();
    }

    private String manejarMaxJugadores(Mensaje mensaje, Jugador jugador) {
        if (mensaje.getDatos() == null) {
            return error("El valor de max jugadores no puede ser null");
        }

        int max;
        try {
            if (mensaje.getDatos() instanceof Double) {
                max = ((Double) mensaje.getDatos()).intValue();
            } else if (mensaje.getDatos() instanceof Integer) {
                max = (Integer) mensaje.getDatos();
            } else {
                return error("Formato de max jugadores inválido");
            }
        } catch (Exception e) {
            return error("Error parseando max jugadores: " + e.getMessage());
        }

        ValidationResult maxResult = InputValidator.validateMaxPlayers(max);
        if (!maxResult.isValid()) {
            return error(maxResult.getErrorMessage());
        }

        JuegoManager.getInstance().setMaxJugadores(max);
        System.out.println("Nuevo límite de jugadores: " + max);
        return broadcast();
    }

    private String manejarTirarCarta(Mensaje mensaje, Jugador jugador) {
        String cJson = gson.toJson(mensaje.getDatos());
        Carta cartaTirada = gson.fromJson(cJson, Carta.class);

        if (JuegoManager.getInstance().procesarJugada(jugador, cartaTirada)) {
            return broadcast();
        }
        return error("Movimiento inválido");
    }

    private String manejarComodin(Mensaje mensaje, Jugador jugador) {
        try {
            JsonArray arr = gson.toJsonTree(mensaje.getDatos()).getAsJsonArray();

            if (arr == null || arr.size() != 2) {
                return error("Formato de comodín inválido (se esperan 2 elementos)");
            }

            Carta comodin = gson.fromJson(arr.get(0), Carta.class);
            String colorStr = arr.get(1).getAsString();

            ValidationResult colorResult = InputValidator.validateColor(colorStr);
            if (!colorResult.isValid()) {
                return error(colorResult.getErrorMessage());
            }

            Color colorElegido = Color.valueOf(colorStr.toUpperCase());

            if (JuegoManager.getInstance().procesarJugadaComodin(jugador, comodin, colorElegido)) {
                return broadcast();
            }
            return error("Movimiento inválido con comodín");

        } catch (Exception e) {
            System.err.println("Error procesando comodín: " + e.getMessage());
            return error("Error procesando comodín: " + e.getMessage());
        }
    }

    private String manejarAtraparUno(Mensaje mensaje) {
        if (mensaje.getDatos() == null) {
            return error("Nombre de víctima no puede ser null");
        }
        String nombreVictima = mensaje.getDatos().toString();
        if (JuegoManager.getInstance().atraparUno(nombreVictima)) {
            return broadcast();
        }
        return error("No se puede atrapar a " + nombreVictima
                + " (ya dijo UNO o no tiene 1 carta)");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Hace broadcast del estado actual y devuelve null (la respuesta ya se envió). */
    private String broadcast() {
        String estadoJson = gson.toJson(
                new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual()));
        server.broadcast(estadoJson);
        return null;
    }

    private String error(String msg) {
        return gson.toJson(new Mensaje("ERROR", msg));
    }

    // -------------------------------------------------------------------------
    // Portador de referencia mutable al jugador
    // -------------------------------------------------------------------------

    /**
     * Envuelve la referencia al Jugador de una conexión para que MessageDispatcher
     * pueda actualizarla en CREATE/JOIN sin necesitar retornar múltiples valores.
     */
    public static class JugadorPortador {
        private Jugador jugador;

        public JugadorPortador(Jugador jugador) {
            this.jugador = jugador;
        }

        public Jugador get() { return jugador; }
        public void set(Jugador jugador) { this.jugador = jugador; }
    }
}

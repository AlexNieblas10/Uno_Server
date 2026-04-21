package org.borradoruno.server.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.borradoruno.server.logic.JuegoManager;
import org.borradoruno.server.validation.InputValidator;
import org.borradoruno.server.validation.ValidationResult;
import org.borradoruno.shared.models.*;
import org.borradoruno.shared.network.Mensaje;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private Jugador jugador;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // Validación: Ignorar líneas vacías
                if (inputLine.trim().isEmpty()) {
                    continue;
                }

                try {
                    Mensaje mensaje = gson.fromJson(inputLine, Mensaje.class);

                    // Validación: Verificar que el mensaje no sea null y tenga tipo
                    if (mensaje == null || mensaje.getTipo() == null) {
                        enviarError("Mensaje inválido");
                        continue;
                    }

                    procesarMensaje(mensaje);
                } catch (Exception e) {
                    System.err.println("Error parseando JSON: " + e.getMessage());
                    enviarError("Error de formato JSON");
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado");
        } finally {
            server.removerCliente(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void procesarMensaje(Mensaje mensaje) {
        System.out.println("Comando recibido: " + mensaje.getTipo() + " con datos: " + mensaje.getDatos());
        try {
            switch (mensaje.getTipo()) {
                case "CREATE":
                case "JOIN":
                    // Validación: Verificar que datos no sea null
                    if (mensaje.getDatos() == null) {
                        enviarError("El nombre no puede ser null");
                        return;
                    }

                    String nombre = (String) mensaje.getDatos();

                    // Validación: Nickname
                    ValidationResult nicknameResult = InputValidator.validateNickname(nombre);
                    if (!nicknameResult.isValid()) {
                        enviar(gson.toJson(new Mensaje("ERROR", nicknameResult.getErrorMessage())));
                        return;
                    }

                    // Validación: Unicidad del nickname
                    if (isNicknameTaken(nombre)) {
                        enviar(gson.toJson(new Mensaje("ERROR", "El apodo '" + nombre + "' ya está en uso")));
                        return;
                    }

                    if (mensaje.getTipo().equals("CREATE")) {
                        // Si no hay jugadores, reiniciamos la partida para una nueva sesión limpia
                        if (JuegoManager.getInstance().getPartidaActual().getJugadores().isEmpty()) {
                            JuegoManager.getInstance().iniciarPartida();
                            JuegoManager.getInstance().getPartidaActual().getJugadores().clear();
                            JuegoManager.getInstance().getPartidaActual().setEstado(EstadoPartida.ESPERANDO_JUGADORES);
                        }
                        this.jugador = new Jugador(nombre, socket.getRemoteSocketAddress().toString());
                        JuegoManager.getInstance().agregarJugador(this.jugador);
                        server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    } else {
                        // JOIN
                        Partida p = JuegoManager.getInstance().getPartidaActual();
                        if (p.getJugadores().size() >= p.getMaxJugadores()) {
                            enviar(gson.toJson(new Mensaje("ERROR", "La sala está llena")));
                            return;
                        }
                        this.jugador = new Jugador(nombre, socket.getRemoteSocketAddress().toString());
                        JuegoManager.getInstance().agregarJugador(this.jugador);
                        server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    }
                    break;
                case "SET_MAX_JUGADORES":
                    // Validación: Datos no null
                    if (mensaje.getDatos() == null) {
                        enviarError("El valor de max jugadores no puede ser null");
                        return;
                    }

                    // GSON a veces envía números como Double o Integer, manejamos ambos
                    int max = 4;
                    try {
                        if (mensaje.getDatos() instanceof Double) {
                            max = ((Double) mensaje.getDatos()).intValue();
                        } else if (mensaje.getDatos() instanceof Integer) {
                            max = (Integer) mensaje.getDatos();
                        } else {
                            enviarError("Formato de max jugadores inválido");
                            return;
                        }
                    } catch (Exception e) {
                        enviarError("Error parseando max jugadores: " + e.getMessage());
                        return;
                    }

                    // Validación: Rango de max jugadores
                    ValidationResult maxResult = InputValidator.validateMaxPlayers(max);
                    if (!maxResult.isValid()) {
                        enviar(gson.toJson(new Mensaje("ERROR", maxResult.getErrorMessage())));
                        return;
                    }

                    JuegoManager.getInstance().setMaxJugadores(max);
                    System.out.println("Nuevo límite de jugadores: " + max);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "INICIAR_PARTIDA":
                    JuegoManager.getInstance().iniciarPartida();
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "TIRAR_CARTA":
                    // Convertimos explícitamente los datos a un objeto Carta
                    String cJson = gson.toJson(mensaje.getDatos());
                    org.borradoruno.shared.models.Carta cartaTirada = gson.fromJson(cJson, org.borradoruno.shared.models.Carta.class);

                    JuegoManager.getInstance().procesarJugada(this.jugador, cartaTirada);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "TIRAR_COMODIN":
                    try {
                        // El mensaje contiene [Carta, Color]
                        JsonArray arr = gson.toJsonTree(mensaje.getDatos()).getAsJsonArray();

                        // Validación: Verificar tamaño del array
                        if (arr == null || arr.size() != 2) {
                            enviarError("Formato de comodín inválido (se esperan 2 elementos)");
                            return;
                        }

                        Carta comodin = gson.fromJson(arr.get(0), Carta.class);
                        String colorStr = arr.get(1).getAsString();

                        // Validación: Color válido con try-catch
                        ValidationResult colorResult = InputValidator.validateColor(colorStr);
                        if (!colorResult.isValid()) {
                            enviar(gson.toJson(new Mensaje("ERROR", colorResult.getErrorMessage())));
                            return;
                        }

                        Color colorElegido = Color.valueOf(colorStr.toUpperCase());

                        JuegoManager.getInstance().procesarJugadaComodin(this.jugador, comodin, colorElegido);
                        server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));

                    } catch (Exception e) {
                        System.err.println("Error procesando comodín: " + e.getMessage());
                        enviarError("Error procesando comodín: " + e.getMessage());
                        return;
                    }
                    break;
                case "ROBAR_CARTA":
                    JuegoManager.getInstance().robarCarta(this.jugador);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "DECIR_UNO":
                    JuegoManager.getInstance().marcarUno(this.jugador);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    System.out.println(this.jugador.getNombre() + " dijo UNO!");
                    break;
                case "ABANDONAR_SALA":
                    JuegoManager.getInstance().removerJugador(this.jugador);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "SOLICITAR_ESTADO":
                    enviar(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void enviar(String mensajeJson) {
        if (out != null) {
            out.println(mensajeJson);
        }
    }

    /**
     * Método helper para enviar mensajes de error al cliente
     */
    private void enviarError(String mensajeError) {
        enviar(gson.toJson(new Mensaje("ERROR", mensajeError)));
    }

    /**
     * Verifica si un nickname ya está siendo usado por otro jugador
     */
    private boolean isNicknameTaken(String nombre) {
        Partida partida = JuegoManager.getInstance().getPartidaActual();
        if (partida == null || partida.getJugadores() == null) {
            return false;
        }
        return partida.getJugadores()
                .stream()
                .anyMatch(j -> j.getNombre().equalsIgnoreCase(nombre));
    }
}
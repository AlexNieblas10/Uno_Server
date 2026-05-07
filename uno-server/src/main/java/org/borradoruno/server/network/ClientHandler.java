package org.borradoruno.server.network;

import com.google.gson.Gson;
import org.borradoruno.shared.models.Jugador;
import org.borradoruno.shared.network.Mensaje;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Maneja la comunicación TCP con un cliente.
 * Solo lee/escribe del socket y delega toda lógica a MessageDispatcher.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Server server;
    private final Gson gson;
    private final MessageDispatcher dispatcher;
    private final MessageDispatcher.JugadorPortador jugadorPortador;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.gson = new Gson();
        this.dispatcher = new MessageDispatcher(server);
        this.jugadorPortador = new MessageDispatcher.JugadorPortador(null);
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String linea;
            while ((linea = in.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;

                try {
                    Mensaje mensaje = gson.fromJson(linea, Mensaje.class);

                    if (mensaje == null || mensaje.getTipo() == null) {
                        enviarError("Mensaje inválido");
                        continue;
                    }

                    System.out.println("Comando recibido: " + mensaje.getTipo()
                            + " con datos: " + mensaje.getDatos());

                    String sesionId = socket.getRemoteSocketAddress().toString();
                    String respuesta = dispatcher.despachar(mensaje, jugadorPortador, sesionId);

                    if (respuesta != null) {
                        enviar(respuesta);
                    }

                } catch (Exception e) {
                    System.err.println("Error parseando JSON: " + e.getMessage());
                    enviarError("Error de formato JSON");
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado");
        } finally {
            server.removerCliente(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void enviar(String mensajeJson) {
        if (out != null) out.println(mensajeJson);
    }

    private void enviarError(String mensajeError) {
        enviar(gson.toJson(new Mensaje("ERROR", mensajeError)));
    }

    public Jugador getJugador() {
        return jugadorPortador.get();
    }
}

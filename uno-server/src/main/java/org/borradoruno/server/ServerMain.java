package org.borradoruno.server;

import org.borradoruno.server.network.Server;

public class ServerMain {
    public static void main(String[] args) {
        // Leer puerto de variable de entorno (Railway lo configura automáticamente)
        // Default a 12345 para desarrollo local
        String portStr = System.getenv("PORT");
        int port = 12345;

        if (portStr != null && !portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.err.println("Puerto inválido en variable PORT: " + portStr + ". Usando default 12345.");
            }
        }

        System.out.println("=== UNO Game Server ===");
        System.out.println("Iniciando servidor en puerto: " + port);

        Server server = new Server(port);
        server.start();
    }
}

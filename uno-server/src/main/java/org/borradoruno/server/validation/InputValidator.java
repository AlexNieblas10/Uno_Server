package org.borradoruno.server.validation;

import org.borradoruno.shared.models.Color;
import org.borradoruno.shared.models.Valor;

public class InputValidator {

    /**
     * Valida un nickname de jugador.
     * Reglas:
     * - No puede ser null o vacío
     * - Longitud entre 3 y 20 caracteres
     * - Solo alfanuméricos, guión y underscore
     */
    public static ValidationResult validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return ValidationResult.failure("NICKNAME_EMPTY", "El apodo no puede estar vacío");
        }

        if (nickname.length() < 3 || nickname.length() > 20) {
            return ValidationResult.failure("NICKNAME_LENGTH",
                "El apodo debe tener entre 3 y 20 caracteres");
        }

        if (!nickname.matches("^[a-zA-Z0-9_-]+$")) {
            return ValidationResult.failure("NICKNAME_INVALID_CHARS",
                "El apodo solo puede contener letras, números, guión y guión bajo");
        }

        return ValidationResult.success();
    }

    /**
     * Valida el número máximo de jugadores.
     * Rango permitido: 2-10
     */
    public static ValidationResult validateMaxPlayers(int max) {
        if (max < 2 || max > 10) {
            return ValidationResult.failure("MAX_PLAYERS_BOUNDS",
                "El máximo de jugadores debe estar entre 2 y 10");
        }
        return ValidationResult.success();
    }

    /**
     * Valida un string de color y convierte a enum.
     * Protege contra IllegalArgumentException de valueOf()
     */
    public static ValidationResult validateColor(String colorStr) {
        if (colorStr == null) {
            return ValidationResult.failure("COLOR_NULL", "El color no puede ser null");
        }

        try {
            Color.valueOf(colorStr.toUpperCase());
            return ValidationResult.success();
        } catch (IllegalArgumentException e) {
            return ValidationResult.failure("COLOR_INVALID",
                "Color inválido: " + colorStr + ". Colores válidos: ROJO, AMARILLO, VERDE, AZUL, NEGRO");
        }
    }

    /**
     * Valida un string de valor y convierte a enum.
     * Protege contra IllegalArgumentException de valueOf()
     */
    public static ValidationResult validateValor(String valorStr) {
        if (valorStr == null) {
            return ValidationResult.failure("VALOR_NULL", "El valor no puede ser null");
        }

        try {
            Valor.valueOf(valorStr.toUpperCase());
            return ValidationResult.success();
        } catch (IllegalArgumentException e) {
            return ValidationResult.failure("VALOR_INVALID",
                "Valor de carta inválido: " + valorStr);
        }
    }

    /**
     * Valida el formato de una dirección IP IPv4.
     * Usado principalmente en el cliente.
     */
    public static ValidationResult validateIPAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return ValidationResult.failure("IP_EMPTY", "La dirección IP no puede estar vacía");
        }

        // Permitir localhost
        if (ip.equals("localhost") || ip.equals("127.0.0.1")) {
            return ValidationResult.success();
        }

        // Patrón IPv4
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                           "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        if (!ip.matches(ipv4Pattern)) {
            return ValidationResult.failure("IP_INVALID_FORMAT",
                "Formato de dirección IP inválido");
        }

        return ValidationResult.success();
    }

    /**
     * Valida un número de puerto.
     * Rango permitido: 1024-65535 (puertos no privilegiados)
     */
    public static ValidationResult validatePort(int port) {
        if (port < 1024 || port > 65535) {
            return ValidationResult.failure("PORT_OUT_OF_RANGE",
                "El puerto debe estar entre 1024 y 65535");
        }
        return ValidationResult.success();
    }
}

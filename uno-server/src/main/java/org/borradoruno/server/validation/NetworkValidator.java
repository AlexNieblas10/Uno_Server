package org.borradoruno.server.validation;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class NetworkValidator {

    /**
     * Valida y parsea JSON a un objeto de clase específica.
     * Maneja errores de deserialización de forma segura.
     */
    public static <T> ValidationResult validateAndParseJSON(String json, Class<T> clazz, Gson gson) {
        if (json == null || json.trim().isEmpty()) {
            return ValidationResult.failure("JSON_EMPTY", "El JSON no puede estar vacío");
        }

        if (gson == null) {
            return ValidationResult.failure("GSON_NULL", "El parseador Gson no puede ser null");
        }

        if (clazz == null) {
            return ValidationResult.failure("CLASS_NULL", "La clase destino no puede ser null");
        }

        try {
            T object = gson.fromJson(json, clazz);
            if (object == null) {
                return ValidationResult.failure("JSON_NULL_RESULT",
                    "El JSON resultó en un objeto null");
            }
            // No podemos devolver el objeto validado en ValidationResult simple
            // Esta sería una versión genérica que solo valida
            return ValidationResult.success();
        } catch (JsonSyntaxException e) {
            return ValidationResult.failure("JSON_SYNTAX_ERROR",
                "Error de sintaxis en JSON: " + e.getMessage());
        } catch (Exception e) {
            return ValidationResult.failure("JSON_PARSE_ERROR",
                "Error parseando JSON: " + e.getMessage());
        }
    }

    /**
     * Valida que un mensaje no sea null o vacío.
     */
    public static ValidationResult validateMessage(String message) {
        if (message == null) {
            return ValidationResult.failure("MESSAGE_NULL", "El mensaje no puede ser null");
        }

        if (message.trim().isEmpty()) {
            return ValidationResult.failure("MESSAGE_EMPTY", "El mensaje no puede estar vacío");
        }

        return ValidationResult.success();
    }

    /**
     * Valida que un tipo de mensaje sea reconocido.
     */
    public static ValidationResult validateMessageType(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return ValidationResult.failure("MESSAGE_TYPE_NULL",
                "El tipo de mensaje no puede ser null o vacío");
        }

        // Lista de tipos de mensaje válidos
        String[] validTypes = {
            "CREATE", "JOIN", "SET_MAX_JUGADORES", "INICIAR_PARTIDA",
            "TIRAR_CARTA", "TIRAR_COMODIN", "ROBAR_CARTA", "DECIR_UNO",
            "ABANDONAR_SALA", "SOLICITAR_ESTADO", "ERROR", "ESTADO_PARTIDA"
        };

        for (String validType : validTypes) {
            if (tipo.equals(validType)) {
                return ValidationResult.success();
            }
        }

        return ValidationResult.failure("MESSAGE_TYPE_INVALID",
            "Tipo de mensaje no reconocido: " + tipo);
    }
}

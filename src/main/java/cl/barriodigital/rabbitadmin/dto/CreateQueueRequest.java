package cl.barriodigital.rabbitadmin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cola. {@code durable} es true por defecto y {@code autoDelete} false.
 * El TTL de mensajes va en milisegundos.
 */
public record CreateQueueRequest(
        @NotNull(message = ValidationRules.NAME_REQUIRED_MESSAGE)
        @Pattern(regexp = ValidationRules.RESOURCE_NAME_REGEX, message = ValidationRules.RESOURCE_NAME_MESSAGE)
        String name,
        Boolean durable,
        Boolean autoDelete,
        @Pattern(regexp = ValidationRules.EXISTING_NAME_REGEX, message = ValidationRules.EXISTING_NAME_MESSAGE)
        String deadLetterExchange,
        @Size(max = ValidationRules.MAX_LENGTH, message = ValidationRules.ROUTING_KEY_MESSAGE)
        String deadLetterRoutingKey,
        @Positive(message = "El TTL debe ser un número positivo de milisegundos")
        Integer messageTtlMs) {
}

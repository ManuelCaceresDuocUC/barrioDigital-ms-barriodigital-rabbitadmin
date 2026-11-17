package cl.barriodigital.rabbitadmin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * QUÉ ES: DTO de entrada para crear un exchange. type define cómo reparte: DIRECT (clave exacta), TOPIC (patrones),
 * FANOUT (a todas las colas), HEADERS (por cabeceras).
 * Datos para crear un exchange. {@code durable} es true por defecto y {@code autoDelete} false.
 */
public record CreateExchangeRequest(
        @NotNull(message = ValidationRules.NAME_REQUIRED_MESSAGE)
        @Pattern(regexp = ValidationRules.RESOURCE_NAME_REGEX, message = ValidationRules.RESOURCE_NAME_MESSAGE)
        String name,
        @NotNull(message = "El tipo de exchange es obligatorio (DIRECT, TOPIC, FANOUT o HEADERS)")
        ExchangeType type,
        Boolean durable,
        Boolean autoDelete) {
}

package cl.barriodigital.rabbitadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Mensaje que un microservicio pide publicar. El servicio completa el sobre con {@code eventId} y
 * {@code timestamp}; quien llama solo aporta el contenido y los datos de trazabilidad.
 */
public record PublishMessageRequest(
        @NotBlank(message = "La routing key es obligatoria")
        @Size(max = ValidationRules.MAX_LENGTH, message = ValidationRules.ROUTING_KEY_MESSAGE)
        String routingKey,
        @NotBlank(message = "El tipo de evento es obligatorio")
        @Size(max = 100, message = "El tipo de evento no puede superar los 100 caracteres")
        String type,
        @Size(max = ValidationRules.MAX_LENGTH, message = "El correlationId no puede superar los 255 caracteres")
        String correlationId,
        @Size(max = ValidationRules.MAX_LENGTH, message = "El traceId no puede superar los 255 caracteres")
        String traceId,
        @NotNull(message = "El payload es obligatorio")
        Map<String, Object> payload) {
}

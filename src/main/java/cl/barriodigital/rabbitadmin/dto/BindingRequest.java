package cl.barriodigital.rabbitadmin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * QUÉ ES: DTO de entrada para crear/eliminar un binding. La routing key es la "etiqueta" que decide qué mensajes
 * del exchange llegan a la cola. Una routing key nula equivale a vacía.
 */
public record BindingRequest(
        @NotNull(message = "El exchange es obligatorio")
        @Pattern(regexp = ValidationRules.RESOURCE_NAME_REGEX, message = ValidationRules.RESOURCE_NAME_MESSAGE)
        String exchange,
        @NotNull(message = "La cola es obligatoria")
        @Pattern(regexp = ValidationRules.RESOURCE_NAME_REGEX, message = ValidationRules.RESOURCE_NAME_MESSAGE)
        String queue,
        @Size(max = ValidationRules.MAX_LENGTH, message = ValidationRules.ROUTING_KEY_MESSAGE)
        String routingKey) {
}

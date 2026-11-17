package cl.barriodigital.rabbitadmin.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * QUÉ ES: DTO de salida con el estado de una cola (mensajes, consumidores, estado, si es protegida).
 * Estado de una cola. Los campos que el Management API aún no conoce (por ejemplo, una cola recién
 * creada) se omiten en lugar de inventarse.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueResponse(
        String name,
        Boolean durable,
        Boolean autoDelete,
        Map<String, Object> arguments,
        long messages,
        int consumers,
        String state,
        boolean protectedResource) {
}

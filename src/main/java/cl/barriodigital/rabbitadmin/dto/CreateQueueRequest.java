package cl.barriodigital.rabbitadmin.dto;

/**
 * Datos para crear una cola. {@code durable} es true por defecto y {@code autoDelete} false.
 * El TTL de mensajes va en milisegundos.
 */
public record CreateQueueRequest(
        String name,
        Boolean durable,
        Boolean autoDelete,
        String deadLetterExchange,
        String deadLetterRoutingKey,
        Integer messageTtlMs) {
}

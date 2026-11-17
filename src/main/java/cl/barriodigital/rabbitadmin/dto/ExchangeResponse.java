package cl.barriodigital.rabbitadmin.dto;

/** QUÉ ES: DTO de salida de un exchange; protectedResource = true si es del sistema (no se puede eliminar). */
public record ExchangeResponse(
        String name,
        String type,
        boolean durable,
        boolean autoDelete,
        boolean protectedResource) {
}

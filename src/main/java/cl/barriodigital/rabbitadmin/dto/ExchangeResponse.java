package cl.barriodigital.rabbitadmin.dto;

public record ExchangeResponse(
        String name,
        String type,
        boolean durable,
        boolean autoDelete,
        boolean protectedResource) {
}

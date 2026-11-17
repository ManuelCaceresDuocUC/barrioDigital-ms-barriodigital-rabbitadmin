package cl.barriodigital.rabbitadmin.dto;

/** QUÉ ES: DTO de salida: un binding (exchange, cola y routing key). */
public record BindingResponse(String exchange, String queue, String routingKey) {
}

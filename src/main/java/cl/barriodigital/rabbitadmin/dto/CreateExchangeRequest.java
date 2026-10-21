package cl.barriodigital.rabbitadmin.dto;

/** Datos para crear un exchange. {@code durable} es true por defecto y {@code autoDelete} false. */
public record CreateExchangeRequest(
        String name,
        ExchangeType type,
        Boolean durable,
        Boolean autoDelete) {
}

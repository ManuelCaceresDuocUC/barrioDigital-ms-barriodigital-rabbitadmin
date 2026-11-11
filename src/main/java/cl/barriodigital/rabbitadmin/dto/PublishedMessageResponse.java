package cl.barriodigital.rabbitadmin.dto;

/** Mensaje aceptado: identifica el evento publicado para poder seguirlo en los logs de los consumidores. */
public record PublishedMessageResponse(String eventId, String exchange, String routingKey) {
}

package cl.barriodigital.rabbitadmin.dto;

/** QUÉ ES: DTO de salida de la publicación. Mensaje aceptado: identifica el evento publicado para poder seguirlo en los logs de los consumidores. */
public record PublishedMessageResponse(String eventId, String exchange, String routingKey) {
}

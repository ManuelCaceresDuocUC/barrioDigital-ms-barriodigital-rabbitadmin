package cl.barriodigital.rabbitadmin.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * QUÉ ES: el "sobre" estándar de todos los eventos: tipo, id único, hora, ids de trazabilidad y el contenido (payload).
 * Sobre común de los eventos que viajan por RabbitMQ. Debe coincidir con el que espera notify.
 */
public record EventEnvelope(
        String type,
        String eventId,
        String timestamp,
        String traceId,
        String correlationId,
        Map<String, Object> payload) {

    // Fábrica: arma el sobre a partir de la petición.
    /** Crea el sobre con un {@code eventId} nuevo y la hora actual; si falta el traceId se genera uno. */
    public static EventEnvelope of(PublishMessageRequest request) {
        // Si el llamador no envía traceId se genera uno para poder seguir el mensaje en los logs.
        String traceId = request.traceId() == null || request.traceId().isBlank()
                ? UUID.randomUUID().toString()
                : request.traceId();
        return new EventEnvelope(request.type(), UUID.randomUUID().toString(), Instant.now().toString(),
                traceId, request.correlationId(), request.payload());
    }
}

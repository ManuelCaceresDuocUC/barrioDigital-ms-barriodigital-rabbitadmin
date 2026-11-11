package cl.barriodigital.rabbitadmin.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Sobre común de los eventos que viajan por RabbitMQ. Debe coincidir con el que espera notify. */
public record EventEnvelope(
        String type,
        String eventId,
        String timestamp,
        String traceId,
        String correlationId,
        Map<String, Object> payload) {

    /** Crea el sobre con un {@code eventId} nuevo y la hora actual; si falta el traceId se genera uno. */
    public static EventEnvelope of(PublishMessageRequest request) {
        String traceId = request.traceId() == null || request.traceId().isBlank()
                ? UUID.randomUUID().toString()
                : request.traceId();
        return new EventEnvelope(request.type(), UUID.randomUUID().toString(), Instant.now().toString(),
                traceId, request.correlationId(), request.payload());
    }
}

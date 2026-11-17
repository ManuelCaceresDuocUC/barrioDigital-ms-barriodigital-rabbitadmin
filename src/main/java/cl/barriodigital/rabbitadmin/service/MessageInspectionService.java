package cl.barriodigital.rabbitadmin.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.PatternMatchUtils;

import cl.barriodigital.rabbitadmin.client.ManagementMessage;
import cl.barriodigital.rabbitadmin.client.RabbitManagementClient;
import cl.barriodigital.rabbitadmin.config.InspectionProperties;
import cl.barriodigital.rabbitadmin.dto.DeathInfo;
import cl.barriodigital.rabbitadmin.dto.DlqMessageResponse;
import cl.barriodigital.rabbitadmin.exception.InspectionNotAllowedException;
import cl.barriodigital.rabbitadmin.exception.InvalidTopologyException;
import cl.barriodigital.rabbitadmin.exception.QueueNotFoundException;
import cl.barriodigital.rabbitadmin.util.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QUÉ ES: servicio que permite ver los mensajes de las DLQ (colas de mensajes fallidos) sin consumirlos.
 * Lectura de los mensajes de las DLQ sin consumirlos: los mensajes se devuelven a la cola (con la
 * marca de "reentregado" que RabbitMQ les pone al leerlos). Solo se permite en las colas indicadas
 * en {@link InspectionProperties}, porque en una cola con consumidores activos la lectura podría
 * interferir con ellos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageInspectionService {

    // Cliente del Management API (único que puede leer mensajes sin consumirlos).
    private final RabbitManagementClient management;
    private final InspectionProperties properties;

    // Lee hasta 'count' mensajes. Orden de validaciones: cola permitida (403) -> count válido (400) -> cola existe (404).
    public List<DlqMessageResponse> peek(String queue, Integer count) {
        if (!isInspectable(queue)) {
            throw new InspectionNotAllowedException(queue);
        }
        int requested = count == null ? properties.defaultMessages() : count;
        if (requested < 1 || requested > properties.maxMessages()) {
            throw new InvalidTopologyException(
                    "El parámetro 'count' debe estar entre 1 y " + properties.maxMessages());
        }
        if (management.findQueue(queue).isEmpty()) {
            throw new QueueNotFoundException(queue);
        }

        List<ManagementMessage> messages = management.peekMessages(queue, requested, properties.maxPayloadBytes());
        log.info("Lectura de {} mensaje(s) de la cola {}", messages.size(), queue);
        return messages.stream().map(this::toResponse).toList();
    }

    // Solo las colas que calzan con inspection.queues (por defecto *.dlq).
    private boolean isInspectable(String queue) {
        return queue != null && PatternMatchUtils.simpleMatch(properties.queues().toArray(String[]::new), queue);
    }

    // Convierte el mensaje del broker en DTO: enmascara correo/RUT y no entrega contenido binario.
    private DlqMessageResponse toResponse(ManagementMessage message) {
        boolean binary = "base64".equalsIgnoreCase(message.payloadEncoding());
        int bytes = message.payloadBytes() == null ? 0 : message.payloadBytes();
        String payload = binary ? null : SensitiveDataMasker.mask(message.payload());
        return new DlqMessageResponse(message.exchange(), message.routingKey(),
                Boolean.TRUE.equals(message.redelivered()), payload, binary, bytes,
                bytes > properties.maxPayloadBytes(), deathOf(message));
    }

    /** Primer registro de {@code x-death} (el más reciente); null si el mensaje no pasó por un dead-letter. */
    private DeathInfo deathOf(ManagementMessage message) {
        // Navega con cuidado properties -> headers -> x-death[0]; cualquier eslabón ausente devuelve null.
        Map<String, Object> props = message.properties();
        if (props == null || !(props.get("headers") instanceof Map<?, ?> headers)) {
            return null;
        }
        if (!(headers.get("x-death") instanceof List<?> deaths) || deaths.isEmpty()
                || !(deaths.get(0) instanceof Map<?, ?> first)) {
            return null;
        }
        return new DeathInfo(text(first.get("queue")), text(first.get("exchange")),
                routingKeys(first.get("routing-keys")), text(first.get("reason")),
                first.get("count") instanceof Number n ? n.longValue() : 0L,
                first.get("time") instanceof Number t ? Instant.ofEpochSecond(t.longValue()) : null);
    }

    // Valor a texto, tolerando null.
    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    // Lista de routing keys del x-death a lista de texto.
    private static List<String> routingKeys(Object value) {
        return value instanceof List<?> keys ? keys.stream().map(String::valueOf).toList() : List.of();
    }
}

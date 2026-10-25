package cl.barriodigital.rabbitadmin.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Reglas para leer el contenido de los mensajes de una cola sin consumirlos.
 *
 * @param queues          nombres (con comodín *) de las colas cuyo contenido se puede leer; por defecto, ninguna
 * @param defaultMessages cantidad de mensajes que se leen si el cliente no indica {@code count}
 * @param maxMessages     máximo de mensajes por lectura
 * @param maxPayloadBytes máximo de bytes del cuerpo de cada mensaje que se entrega
 */
@ConfigurationProperties(prefix = "barriodigital.rabbitadmin.inspection")
public record InspectionProperties(List<String> queues, Integer defaultMessages, Integer maxMessages,
                                   Integer maxPayloadBytes) {

    public InspectionProperties {
        queues = queues == null ? List.of() : List.copyOf(queues);
        defaultMessages = defaultMessages == null ? 20 : defaultMessages;
        maxMessages = maxMessages == null ? 50 : maxMessages;
        maxPayloadBytes = maxPayloadBytes == null ? 20_000 : maxPayloadBytes;
        Assert.isTrue(defaultMessages >= 1, "barriodigital.rabbitadmin.inspection.default-messages debe ser al menos 1");
        Assert.isTrue(maxMessages >= defaultMessages,
                "barriodigital.rabbitadmin.inspection.max-messages no puede ser menor que default-messages");
        Assert.isTrue(maxPayloadBytes >= 1, "barriodigital.rabbitadmin.inspection.max-payload-bytes debe ser positivo");
    }
}

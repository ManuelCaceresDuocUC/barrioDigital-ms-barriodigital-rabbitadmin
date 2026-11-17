package cl.barriodigital.rabbitadmin.service;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import cl.barriodigital.rabbitadmin.config.PublishingProperties;
import cl.barriodigital.rabbitadmin.dto.EventEnvelope;
import cl.barriodigital.rabbitadmin.dto.PublishMessageRequest;
import cl.barriodigital.rabbitadmin.dto.PublishedMessageResponse;
import cl.barriodigital.rabbitadmin.exception.BrokerOperationException;
import cl.barriodigital.rabbitadmin.exception.InvalidMessageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QUÉ ES: servicio que publica mensajes en RabbitMQ para los demás microservicios.
 * Único punto por el que los microservicios publican en RabbitMQ: la conexión, el formato JSON, el
 * exchange y las routing keys permitidas viven aquí y no se repiten en cada servicio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisherService {

    // Plantilla AMQP configurada en RabbitAdminConfig.
    private final RabbitTemplate rabbitTemplate;
    private final PublishingProperties publishing;

    // 1) valida la routing key, 2) arma el sobre, 3) publica, 4) devuelve el eventId.
    public PublishedMessageResponse publish(PublishMessageRequest request) {
        if (!publishing.allows(request.routingKey())) {
            throw new InvalidMessageException("La routing key '" + request.routingKey() + "' no está permitida");
        }
        // El sobre añade eventId único y hora.
        EventEnvelope envelope = EventEnvelope.of(request);
        try {
            rabbitTemplate.convertAndSend(publishing.exchange(), request.routingKey(), envelope);
        } catch (AmqpException ex) {
            throw new BrokerOperationException("No se pudo publicar el mensaje en RabbitMQ", ex);
        }
        log.info("Mensaje publicado: exchange={}, routingKey={}, type={}, eventId={}, correlationId={}",
                publishing.exchange(), request.routingKey(), envelope.type(), envelope.eventId(),
                envelope.correlationId());
        return new PublishedMessageResponse(envelope.eventId(), publishing.exchange(), request.routingKey());
    }
}

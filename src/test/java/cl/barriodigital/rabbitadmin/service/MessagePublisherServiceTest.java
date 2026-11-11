package cl.barriodigital.rabbitadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import cl.barriodigital.rabbitadmin.config.PublishingProperties;
import cl.barriodigital.rabbitadmin.dto.EventEnvelope;
import cl.barriodigital.rabbitadmin.dto.PublishMessageRequest;
import cl.barriodigital.rabbitadmin.dto.PublishedMessageResponse;
import cl.barriodigital.rabbitadmin.exception.BrokerOperationException;
import cl.barriodigital.rabbitadmin.exception.InvalidMessageException;

class MessagePublisherServiceTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final PublishingProperties publishing =
            new PublishingProperties("cmd.direct", List.of("email.send", "crew.ticket", "certificate.gen"));
    private final MessagePublisherService service = new MessagePublisherService(rabbitTemplate, publishing);

    private static PublishMessageRequest request(String routingKey, String traceId) {
        return new PublishMessageRequest(routingKey, "CREW_TICKET", "15", traceId, Map.of("ticketId", "15"));
    }

    @Test
    void publicaElSobreEnElExchangeConfigurado() {
        PublishedMessageResponse response = service.publish(request("crew.ticket", "trace-1"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("crew.ticket"), captor.capture());
        EventEnvelope envelope = (EventEnvelope) captor.getValue();
        assertThat(envelope.type()).isEqualTo("CREW_TICKET");
        assertThat(envelope.correlationId()).isEqualTo("15");
        assertThat(envelope.traceId()).isEqualTo("trace-1");
        assertThat(envelope.payload()).containsEntry("ticketId", "15");
        assertThat(envelope.eventId()).isNotBlank();
        assertThat(envelope.timestamp()).isNotBlank();
        assertThat(response.eventId()).isEqualTo(envelope.eventId());
        assertThat(response.exchange()).isEqualTo("cmd.direct");
        assertThat(response.routingKey()).isEqualTo("crew.ticket");
    }

    @Test
    void generaUnTraceIdSiNoLlega() {
        service.publish(request("crew.ticket", null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture());
        assertThat(((EventEnvelope) captor.getValue()).traceId()).isNotBlank();
    }

    @Test
    void cadaPublicacionTieneUnEventIdDistinto() {
        PublishedMessageResponse first = service.publish(request("crew.ticket", "t"));
        PublishedMessageResponse second = service.publish(request("crew.ticket", "t"));

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
    }

    @Test
    void rechazaUnaRoutingKeyQueNoEstaPermitida() {
        assertThatThrownBy(() -> service.publish(request("admin.borrar", "t")))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining("admin.borrar");

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void unaFallaDelBrokerSeTraduceAUnErrorDeDominio() {
        doThrow(new AmqpConnectException(new IOException("broker caído")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatThrownBy(() -> service.publish(request("crew.ticket", "t")))
                .isInstanceOf(BrokerOperationException.class);
    }
}

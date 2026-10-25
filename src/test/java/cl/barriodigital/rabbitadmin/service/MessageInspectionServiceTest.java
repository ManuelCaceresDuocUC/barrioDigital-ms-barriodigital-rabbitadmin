package cl.barriodigital.rabbitadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import cl.barriodigital.rabbitadmin.client.ManagementMessage;
import cl.barriodigital.rabbitadmin.client.ManagementQueue;
import cl.barriodigital.rabbitadmin.client.RabbitManagementClient;
import cl.barriodigital.rabbitadmin.config.InspectionProperties;
import cl.barriodigital.rabbitadmin.dto.DlqMessageResponse;
import cl.barriodigital.rabbitadmin.exception.InspectionNotAllowedException;
import cl.barriodigital.rabbitadmin.exception.InvalidTopologyException;
import cl.barriodigital.rabbitadmin.exception.QueueNotFoundException;

class MessageInspectionServiceTest {

    private static final String DLQ = "q.cmd.email.dlq";

    private final RabbitManagementClient management = mock(RabbitManagementClient.class);
    private final InspectionProperties properties = new InspectionProperties(List.of("*.dlq"), 20, 50, 1_000);
    private final MessageInspectionService service = new MessageInspectionService(management, properties);

    private void queueExists(String name) {
        when(management.findQueue(name)).thenReturn(Optional.of(
                new ManagementQueue(name, true, false, null, "running", 1, 0)));
    }

    private static ManagementMessage message(String payload, String encoding, int bytes, Map<String, Object> props) {
        return new ManagementMessage("cmd.dead.dlx", "email.dlq", true, bytes, payload, encoding, props);
    }

    @Test
    void leeLosMensajesDeUnaDlqYEnmascaraCorreoYRut() {
        queueExists(DLQ);
        String payload = "{\"to\":\"maria.perez@gmail.com\",\"rut\":\"12.345.678-9\"}";
        when(management.peekMessages(DLQ, 20, 1_000)).thenReturn(List.of(message(payload, "string", 60, null)));

        List<DlqMessageResponse> result = service.peek(DLQ, null);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.payload()).contains("m***@gmail.com").contains("[RUT]")
                    .doesNotContain("maria.perez").doesNotContain("12.345.678");
            assertThat(item.exchange()).isEqualTo("cmd.dead.dlx");
            assertThat(item.routingKey()).isEqualTo("email.dlq");
            assertThat(item.redelivered()).isTrue();
            assertThat(item.binary()).isFalse();
            assertThat(item.payloadTruncated()).isFalse();
            assertThat(item.death()).isNull();
        });
    }

    @Test
    void sinCountLeeLaCantidadPorDefecto() {
        queueExists(DLQ);
        when(management.peekMessages(anyString(), anyInt(), anyInt())).thenReturn(List.of());

        assertThat(service.peek(DLQ, null)).isEmpty();

        verify(management).peekMessages(DLQ, 20, 1_000);
    }

    @Test
    void entregaElMotivoYElOrigenDelFallo() {
        queueExists(DLQ);
        Map<String, Object> death = Map.of("count", 3, "reason", "rejected", "queue", "q.cmd.email",
                "exchange", "cmd.direct", "routing-keys", List.of("email.send"), "time", 1792876000);
        Map<String, Object> props = Map.of("headers", Map.of("x-death", List.of(death)));
        when(management.peekMessages(DLQ, 5, 1_000)).thenReturn(List.of(message("{}", "string", 2, props)));

        DlqMessageResponse item = service.peek(DLQ, 5).get(0);

        assertThat(item.death()).isNotNull();
        assertThat(item.death().queue()).isEqualTo("q.cmd.email");
        assertThat(item.death().exchange()).isEqualTo("cmd.direct");
        assertThat(item.death().routingKeys()).containsExactly("email.send");
        assertThat(item.death().reason()).isEqualTo("rejected");
        assertThat(item.death().count()).isEqualTo(3);
        assertThat(item.death().time()).isEqualTo(Instant.ofEpochSecond(1792876000L));
    }

    @Test
    void unMensajeSinXDeathNoTraeInformacionDeFallo() {
        queueExists(DLQ);
        Map<String, Object> props = Map.of("headers", Map.of("otra", "cabecera"));
        when(management.peekMessages(DLQ, 20, 1_000)).thenReturn(List.of(message("{}", "string", 2, props)));

        assertThat(service.peek(DLQ, null).get(0).death()).isNull();
    }

    @Test
    void noEntregaElContenidoBinario() {
        queueExists(DLQ);
        when(management.peekMessages(DLQ, 20, 1_000)).thenReturn(List.of(message("AAEC", "base64", 3, null)));

        DlqMessageResponse item = service.peek(DLQ, null).get(0);

        assertThat(item.binary()).isTrue();
        assertThat(item.payload()).isNull();
    }

    @Test
    void marcaElContenidoTruncado() {
        queueExists(DLQ);
        when(management.peekMessages(DLQ, 20, 1_000)).thenReturn(List.of(message("{\"a\":", "string", 5_000, null)));

        DlqMessageResponse item = service.peek(DLQ, null).get(0);

        assertThat(item.payloadTruncated()).isTrue();
        assertThat(item.payloadBytes()).isEqualTo(5_000);
    }

    @Test
    void soloPermiteLeerLasColasDeMensajesFallidos() {
        assertThatThrownBy(() -> service.peek("q.cmd.email", 10)).isInstanceOf(InspectionNotAllowedException.class);
        assertThatThrownBy(() -> service.peek("q.prueba", null)).isInstanceOf(InspectionNotAllowedException.class);
        verify(management, never()).peekMessages(anyString(), anyInt(), anyInt());
    }

    @Test
    void rechazaUnCountFueraDeRango() {
        assertThatThrownBy(() -> service.peek(DLQ, 0)).isInstanceOf(InvalidTopologyException.class);
        assertThatThrownBy(() -> service.peek(DLQ, 51)).isInstanceOf(InvalidTopologyException.class);
        verify(management, never()).peekMessages(anyString(), anyInt(), anyInt());
    }

    @Test
    void unaColaQueNoExisteEsUn404() {
        when(management.findQueue("q.otra.dlq")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.peek("q.otra.dlq", 10)).isInstanceOf(QueueNotFoundException.class);
        verify(management, never()).peekMessages(anyString(), anyInt(), anyInt());
    }
}

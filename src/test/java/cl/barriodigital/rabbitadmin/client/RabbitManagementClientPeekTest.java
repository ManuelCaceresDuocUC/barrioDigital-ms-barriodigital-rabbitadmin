package cl.barriodigital.rabbitadmin.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import cl.barriodigital.rabbitadmin.config.ManagementProperties;
import cl.barriodigital.rabbitadmin.exception.ManagementApiException;

class RabbitManagementClientPeekTest {

    private static final String BASE = "http://rabbit:15672";

    private MockRestServiceServer server;
    private RabbitManagementClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RabbitManagementClient(builder.build(),
                new ManagementProperties(BASE, "barrio", "secreto", Duration.ofSeconds(5)));
    }

    @Test
    void leeLosMensajesSinConsumirlosYPidiendoElTruncado() {
        server.expect(requestTo(BASE + "/api/queues/%2F/q.cmd.crew.dlq/get"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.ackmode").value("ack_requeue_true"))
                .andExpect(jsonPath("$.encoding").value("auto"))
                .andExpect(jsonPath("$.truncate").value(20000))
                .andRespond(withSuccess("""
                        [{"payload_bytes":120,"redelivered":true,"exchange":"cmd.dead.dlx",
                          "routing_key":"crew.dlq","message_count":0,"payload":"{\\"ticketId\\":\\"\\"}",
                          "payload_encoding":"string",
                          "properties":{"headers":{"x-death":[{"count":1,"reason":"rejected",
                          "queue":"q.cmd.crew","exchange":"cmd.direct","routing-keys":["crew.ticket"],"time":1792876000}]}},
                          "campo_nuevo":1}]
                        """, MediaType.APPLICATION_JSON));

        List<ManagementMessage> messages = client.peekMessages("q.cmd.crew.dlq", 5, 20000);

        assertThat(messages).singleElement().satisfies(message -> {
            assertThat(message.exchange()).isEqualTo("cmd.dead.dlx");
            assertThat(message.routingKey()).isEqualTo("crew.dlq");
            assertThat(message.redelivered()).isTrue();
            assertThat(message.payloadBytes()).isEqualTo(120);
            assertThat(message.payload()).isEqualTo("{\"ticketId\":\"\"}");
            assertThat(message.payloadEncoding()).isEqualTo("string");
            assertThat(message.properties()).containsKey("headers");
            assertThat(String.valueOf(message.properties().get("headers"))).contains("x-death");
        });
        server.verify();
    }

    @Test
    void unaColaVaciaDevuelveUnaListaVacia() {
        server.expect(requestTo(BASE + "/api/queues/%2F/q.cmd.crew.dlq/get"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.peekMessages("q.cmd.crew.dlq", 20, 20000)).isEmpty();
    }

    @Test
    void unErrorDelServidorSeTraduceAUnaExcepcionDeDominio() {
        server.expect(requestTo(BASE + "/api/queues/%2F/q.cmd.crew.dlq/get")).andRespond(withServerError());

        assertThatThrownBy(() -> client.peekMessages("q.cmd.crew.dlq", 20, 20000))
                .isInstanceOf(ManagementApiException.class);
    }
}

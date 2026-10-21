package cl.barriodigital.rabbitadmin.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import cl.barriodigital.rabbitadmin.config.ManagementProperties;
import cl.barriodigital.rabbitadmin.exception.ManagementApiException;

class RabbitManagementClientTest {

    private static final String BASE = "http://rabbit:15672";

    private MockRestServiceServer server;
    private RabbitManagementClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeaders(headers -> headers.setBasicAuth("barrio", "secreto"));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RabbitManagementClient(builder.build(),
                new ManagementProperties(BASE, "barrio", "secreto", Duration.ofSeconds(5)));
    }

    @Test
    void listaLasColasIgnorandoCamposDesconocidos() {
        server.expect(requestTo(BASE + "/api/queues/%2F"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic " + HttpHeaders.encodeBasicAuth("barrio", "secreto", null)))
                .andRespond(withSuccess("""
                        [{"name":"q.cmd.email","vhost":"/","durable":true,"auto_delete":false,
                          "arguments":{"x-dead-letter-exchange":"cmd.dead.dlx"},"state":"running",
                          "messages":3,"consumers":1,"node":"rabbit@rabbitmq1","campo_nuevo":42}]
                        """, MediaType.APPLICATION_JSON));

        List<ManagementQueue> queues = client.listQueues();

        assertThat(queues).hasSize(1);
        ManagementQueue queue = queues.get(0);
        assertThat(queue.name()).isEqualTo("q.cmd.email");
        assertThat(queue.durable()).isTrue();
        assertThat(queue.autoDelete()).isFalse();
        assertThat(queue.messages()).isEqualTo(3);
        assertThat(queue.consumers()).isEqualTo(1);
        assertThat(queue.arguments()).containsEntry("x-dead-letter-exchange", "cmd.dead.dlx");
        server.verify();
    }

    @Test
    void listaLosBindingsConSusNombresDeCampo() {
        server.expect(requestTo(BASE + "/api/bindings/%2F"))
                .andRespond(withSuccess("""
                        [{"source":"cmd.direct","destination":"q.cmd.email","destination_type":"queue",
                          "routing_key":"email.send","vhost":"/"}]
                        """, MediaType.APPLICATION_JSON));

        List<ManagementBinding> bindings = client.listBindings();

        assertThat(bindings).singleElement().satisfies(binding -> {
            assertThat(binding.source()).isEqualTo("cmd.direct");
            assertThat(binding.destinationType()).isEqualTo("queue");
            assertThat(binding.routingKey()).isEqualTo("email.send");
        });
    }

    @Test
    void codificaElNombreEnLaRutaDeLaCola() {
        server.expect(requestTo(BASE + "/api/queues/%2F/q.con%20espacio"))
                .andRespond(withSuccess("{\"name\":\"q.con espacio\"}", MediaType.APPLICATION_JSON));

        assertThat(client.findQueue("q.con espacio")).isPresent();
    }

    @Test
    void unaColaInexistenteDevuelveVacio() {
        server.expect(requestTo(BASE + "/api/queues/%2F/q.nada")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findQueue("q.nada")).isEmpty();
    }

    @Test
    void exchangeExistenteYNoExistente() {
        server.expect(requestTo(BASE + "/api/exchanges/%2F/cmd.direct"))
                .andRespond(withSuccess("{\"name\":\"cmd.direct\",\"type\":\"direct\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/exchanges/%2F/ex.nada")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.exchangeExists("cmd.direct")).isTrue();
        assertThat(client.exchangeExists("ex.nada")).isFalse();
    }

    @Test
    void unErrorDelServidorSeTraduceAUnaExcepcionDeDominio() {
        server.expect(requestTo(BASE + "/api/queues/%2F")).andRespond(withServerError());

        assertThatThrownBy(client::listQueues).isInstanceOf(ManagementApiException.class);
    }
}

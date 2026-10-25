package cl.barriodigital.rabbitadmin.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

import cl.barriodigital.rabbitadmin.config.ManagementProperties;
import cl.barriodigital.rabbitadmin.exception.ManagementApiException;

/**
 * Cliente del API HTTP de gestión de RabbitMQ. Solo consulta lo que {@code AmqpAdmin} no ofrece:
 * listar colas, exchanges y bindings, y leer sus propiedades. Opera sobre el vhost por defecto ("/").
 */
@Component
public class RabbitManagementClient {

    /** El vhost "/" va codificado en la ruta del API de gestión. */
    private static final String VHOST = "%2F";

    private final RestClient restClient;
    private final String baseUrl;

    public RabbitManagementClient(RestClient managementRestClient, ManagementProperties properties) {
        this.restClient = managementRestClient;
        this.baseUrl = properties.url();
    }

    public List<ManagementQueue> listQueues() {
        return getList("/api/queues/" + VHOST, new ParameterizedTypeReference<>() { });
    }

    public Optional<ManagementQueue> findQueue(String name) {
        return find("/api/queues/" + VHOST + "/" + segment(name), ManagementQueue.class);
    }

    public List<ManagementExchange> listExchanges() {
        return getList("/api/exchanges/" + VHOST, new ParameterizedTypeReference<>() { });
    }

    public boolean exchangeExists(String name) {
        return find("/api/exchanges/" + VHOST + "/" + segment(name), ManagementExchange.class).isPresent();
    }

    public List<ManagementBinding> listBindings() {
        return getList("/api/bindings/" + VHOST, new ParameterizedTypeReference<>() { });
    }

    public List<ManagementBinding> listQueueBindings(String queue) {
        return getList("/api/queues/" + VHOST + "/" + segment(queue) + "/bindings",
                new ParameterizedTypeReference<>() { });
    }

    /**
     * Lee mensajes de una cola sin consumirlos: el Management API los entrega y los devuelve a la cola
     * (modo {@code ack_requeue_true}), marcándolos como reentregados.
     */
    public List<ManagementMessage> peekMessages(String queue, int count, int maxPayloadBytes) {
        String path = "/api/queues/" + VHOST + "/" + segment(queue) + "/get";
        Map<String, Object> request = Map.of(
                "count", count,
                "ackmode", "ack_requeue_true",
                "encoding", "auto",
                "truncate", maxPayloadBytes);
        try {
            List<ManagementMessage> body = restClient.post()
                    .uri(URI.create(baseUrl + path))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ManagementMessage>>() { });
            return body == null ? List.of() : body;
        } catch (RestClientException ex) {
            throw new ManagementApiException("No se pudieron leer los mensajes de la cola '" + queue + "'", ex);
        }
    }

    private <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> type) {
        try {
            List<T> body = restClient.get().uri(URI.create(baseUrl + path)).retrieve().body(type);
            return body == null ? List.of() : body;
        } catch (RestClientException ex) {
            throw new ManagementApiException("No se pudo consultar el Management API de RabbitMQ (" + path + ")", ex);
        }
    }

    private <T> Optional<T> find(String path, Class<T> type) {
        try {
            return Optional.ofNullable(restClient.get().uri(URI.create(baseUrl + path)).retrieve().body(type));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw new ManagementApiException("No se pudo consultar el Management API de RabbitMQ (" + path + ")", ex);
        }
    }

    private static String segment(String name) {
        return UriUtils.encodePathSegment(name, StandardCharsets.UTF_8);
    }
}

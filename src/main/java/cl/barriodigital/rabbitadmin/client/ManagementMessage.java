package cl.barriodigital.rabbitadmin.client;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Un mensaje tal como lo devuelve la operación "get messages" del Management API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagementMessage(
        String exchange,
        @JsonProperty("routing_key") String routingKey,
        Boolean redelivered,
        @JsonProperty("payload_bytes") Integer payloadBytes,
        String payload,
        @JsonProperty("payload_encoding") String payloadEncoding,
        Map<String, Object> properties) {
}

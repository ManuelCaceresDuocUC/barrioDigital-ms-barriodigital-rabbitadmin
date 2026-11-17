package cl.barriodigital.rabbitadmin.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * QUÉ ES: un binding (regla que une un exchange con una cola/exchange) tal como lo devuelve el Management API.
 * Los @JsonProperty mapean los nombres con guion bajo del JSON a campos Java; ignoreUnknown descarta el resto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagementBinding(
        String source,
        String destination,
        @JsonProperty("destination_type") String destinationType,
        @JsonProperty("routing_key") String routingKey) {
}

package cl.barriodigital.rabbitadmin.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagementBinding(
        String source,
        String destination,
        @JsonProperty("destination_type") String destinationType,
        @JsonProperty("routing_key") String routingKey) {
}

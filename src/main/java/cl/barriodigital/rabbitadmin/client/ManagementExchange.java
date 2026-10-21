package cl.barriodigital.rabbitadmin.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagementExchange(
        String name,
        String type,
        Boolean durable,
        @JsonProperty("auto_delete") Boolean autoDelete) {
}

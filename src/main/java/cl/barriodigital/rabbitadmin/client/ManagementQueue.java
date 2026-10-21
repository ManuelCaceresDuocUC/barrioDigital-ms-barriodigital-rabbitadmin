package cl.barriodigital.rabbitadmin.client;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagementQueue(
        String name,
        Boolean durable,
        @JsonProperty("auto_delete") Boolean autoDelete,
        Map<String, Object> arguments,
        String state,
        Integer messages,
        Integer consumers) {
}

package cl.barriodigital.rabbitadmin.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Nombres (con comodín *) de la topología que la API no permite eliminar. */
@ConfigurationProperties(prefix = "barriodigital.rabbitadmin.protection")
public record ProtectionProperties(List<String> queues, List<String> exchanges) {

    public ProtectionProperties {
        queues = queues == null ? List.of() : List.copyOf(queues);
        exchanges = exchanges == null ? List.of() : List.copyOf(exchanges);
    }
}

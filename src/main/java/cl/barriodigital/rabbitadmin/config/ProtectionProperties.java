package cl.barriodigital.rabbitadmin.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * QUÉ ES: lista de colas y exchanges "del sistema" (admiten comodín *, p. ej. q.cmd.*) que la API no deja eliminar.
 * Se consulta desde ProtectedResources.
 */
@ConfigurationProperties(prefix = "barriodigital.rabbitadmin.protection")
public record ProtectionProperties(List<String> queues, List<String> exchanges) {

    public ProtectionProperties {
        queues = queues == null ? List.of() : List.copyOf(queues);
        exchanges = exchanges == null ? List.of() : List.copyOf(exchanges);
    }
}

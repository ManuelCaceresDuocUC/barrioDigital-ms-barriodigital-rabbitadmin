package cl.barriodigital.rabbitadmin.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Dónde y con qué claves publica el servicio los mensajes que le envían los demás microservicios.
 * Centraliza en un solo lugar el exchange y las routing keys permitidas.
 *
 * @param exchange    exchange en el que se publica (por ejemplo, {@code cmd.direct})
 * @param routingKeys únicas routing keys que se aceptan; cualquier otra se rechaza con 400
 */
@ConfigurationProperties(prefix = "barriodigital.rabbitadmin.publishing")
public record PublishingProperties(String exchange, List<String> routingKeys) {

    public PublishingProperties {
        Assert.hasText(exchange, "barriodigital.rabbitadmin.publishing.exchange es obligatorio");
        routingKeys = routingKeys == null ? List.of() : List.copyOf(routingKeys);
        Assert.notEmpty(routingKeys, "barriodigital.rabbitadmin.publishing.routing-keys necesita al menos una clave");
    }

    public boolean allows(String routingKey) {
        return routingKey != null && routingKeys.contains(routingKey);
    }
}

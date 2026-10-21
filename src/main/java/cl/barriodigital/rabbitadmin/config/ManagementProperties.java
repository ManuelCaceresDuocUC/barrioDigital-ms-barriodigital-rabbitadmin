package cl.barriodigital.rabbitadmin.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/** Conexión al API HTTP de gestión de RabbitMQ (puerto 15672). */
@ConfigurationProperties(prefix = "barriodigital.rabbitadmin.management")
public record ManagementProperties(String url, String username, String password, Duration timeout) {

    public ManagementProperties {
        Assert.hasText(url, "barriodigital.rabbitadmin.management.url es obligatoria");
        Assert.hasText(username, "barriodigital.rabbitadmin.management.username es obligatoria");
        Assert.hasText(password, "barriodigital.rabbitadmin.management.password es obligatoria");
        Assert.isTrue(timeout != null && !timeout.isZero() && !timeout.isNegative(),
                "barriodigital.rabbitadmin.management.timeout debe ser una duración positiva");
        url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** No incluye la contraseña, para que nunca llegue a un log. */
    @Override
    public String toString() {
        return "ManagementProperties[url=%s, username=%s, timeout=%s]".formatted(url, username, timeout);
    }
}

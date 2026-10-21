package cl.barriodigital.rabbitadmin.service;

import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;

import cl.barriodigital.rabbitadmin.config.ProtectionProperties;

/** Decide qué colas y exchanges pertenecen a la topología del sistema y no pueden eliminarse. */
@Component
public class ProtectedResources {

    private final String[] queuePatterns;
    private final String[] exchangePatterns;

    public ProtectedResources(ProtectionProperties properties) {
        this.queuePatterns = properties.queues().toArray(String[]::new);
        this.exchangePatterns = properties.exchanges().toArray(String[]::new);
    }

    public boolean isProtectedQueue(String name) {
        return name != null && PatternMatchUtils.simpleMatch(queuePatterns, name);
    }

    /** El exchange por defecto (nombre vacío) tampoco se puede tocar. */
    public boolean isProtectedExchange(String name) {
        return name != null && (name.isBlank() || PatternMatchUtils.simpleMatch(exchangePatterns, name));
    }
}

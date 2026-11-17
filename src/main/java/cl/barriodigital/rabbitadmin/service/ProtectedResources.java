package cl.barriodigital.rabbitadmin.service;

import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;

import cl.barriodigital.rabbitadmin.config.ProtectionProperties;

/**
 * QUÉ ES: componente que decide qué colas y exchanges son "del sistema" y no pueden eliminarse.
 * QUÉ HACE: compara el nombre con los patrones de barriodigital.rabbitadmin.protection (application.yaml).
 */
@Component
public class ProtectedResources {

    // Patrones con comodín * para colas y exchanges.
    private final String[] queuePatterns;
    private final String[] exchangePatterns;

    public ProtectedResources(ProtectionProperties properties) {
        this.queuePatterns = properties.queues().toArray(String[]::new);
        this.exchangePatterns = properties.exchanges().toArray(String[]::new);
    }

    // true si la cola es del sistema.
    public boolean isProtectedQueue(String name) {
        return name != null && PatternMatchUtils.simpleMatch(queuePatterns, name);
    }

    /** El exchange por defecto (nombre vacío) tampoco se puede tocar. */
    public boolean isProtectedExchange(String name) {
        return name != null && (name.isBlank() || PatternMatchUtils.simpleMatch(exchangePatterns, name));
    }
}

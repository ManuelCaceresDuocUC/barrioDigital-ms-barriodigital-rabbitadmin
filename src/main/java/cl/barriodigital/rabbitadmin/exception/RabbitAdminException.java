package cl.barriodigital.rabbitadmin.exception;

/**
 * QUÉ ES: clase abstracta común (RuntimeException: no obliga a declarar throws). Permite agrupar excepciones en GlobalExceptionHandler.
 * Base de las excepciones de dominio del administrador; el handler las traduce a HTTP.
 */
public abstract class RabbitAdminException extends RuntimeException {

    protected RabbitAdminException(String message) {
        super(message);
    }

    // Variante que conserva la causa original para verla en el log.
    protected RabbitAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}

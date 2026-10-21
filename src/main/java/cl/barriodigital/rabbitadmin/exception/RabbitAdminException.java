package cl.barriodigital.rabbitadmin.exception;

/** Base de las excepciones de dominio del administrador; el handler las traduce a HTTP. */
public abstract class RabbitAdminException extends RuntimeException {

    protected RabbitAdminException(String message) {
        super(message);
    }

    protected RabbitAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}

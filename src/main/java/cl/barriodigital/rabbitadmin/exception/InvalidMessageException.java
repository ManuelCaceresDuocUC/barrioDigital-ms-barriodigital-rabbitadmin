package cl.barriodigital.rabbitadmin.exception;

/** QUÉ ES: se traduce a HTTP 400. El mensaje que se pidió publicar no es válido (por ejemplo, una routing key que no está permitida). */
public class InvalidMessageException extends RabbitAdminException {

    public InvalidMessageException(String message) {
        super(message);
    }
}

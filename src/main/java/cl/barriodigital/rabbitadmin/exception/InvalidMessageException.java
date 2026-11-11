package cl.barriodigital.rabbitadmin.exception;

/** El mensaje que se pidió publicar no es válido (por ejemplo, una routing key que no está permitida). */
public class InvalidMessageException extends RabbitAdminException {

    public InvalidMessageException(String message) {
        super(message);
    }
}

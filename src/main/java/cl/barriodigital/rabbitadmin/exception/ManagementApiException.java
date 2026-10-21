package cl.barriodigital.rabbitadmin.exception;

/** El API HTTP de gestión de RabbitMQ no respondió o devolvió un error. */
public class ManagementApiException extends RabbitAdminException {

    public ManagementApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

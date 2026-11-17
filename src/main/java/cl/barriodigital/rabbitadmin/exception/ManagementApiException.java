package cl.barriodigital.rabbitadmin.exception;

/** QUÉ ES: fallo del Management API. Se traduce a HTTP 502. El API HTTP de gestión de RabbitMQ no respondió o devolvió un error. */
public class ManagementApiException extends RabbitAdminException {

    public ManagementApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

package cl.barriodigital.rabbitadmin.exception;

/** QUÉ ES: fallo del broker. Se traduce a HTTP 502. RabbitMQ rechazó o no pudo completar una operación AMQP. */
public class BrokerOperationException extends RabbitAdminException {

    public BrokerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

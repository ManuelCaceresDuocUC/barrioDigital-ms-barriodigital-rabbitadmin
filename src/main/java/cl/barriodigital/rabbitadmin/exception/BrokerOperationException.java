package cl.barriodigital.rabbitadmin.exception;

/** RabbitMQ rechazó o no pudo completar una operación AMQP. */
public class BrokerOperationException extends RabbitAdminException {

    public BrokerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package cl.barriodigital.rabbitadmin.exception;

/** QUÉ ES: excepción "el binding no existe". Se traduce a HTTP 404. */
public class BindingNotFoundException extends RabbitAdminException {

    public BindingNotFoundException(String exchange, String queue, String routingKey) {
        super("No existe un binding del exchange '" + exchange + "' a la cola '" + queue
                + "' con la routing key '" + routingKey + "'");
    }
}

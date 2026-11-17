package cl.barriodigital.rabbitadmin.exception;

/** QUÉ ES: excepción "la cola no existe". Se traduce a HTTP 404. */
public class QueueNotFoundException extends RabbitAdminException {

    public QueueNotFoundException(String name) {
        super("La cola '" + name + "' no existe");
    }
}

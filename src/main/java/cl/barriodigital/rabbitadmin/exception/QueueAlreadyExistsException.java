package cl.barriodigital.rabbitadmin.exception;

/** QUÉ ES: excepción "ya existe una cola con ese nombre". Se traduce a HTTP 409. */
public class QueueAlreadyExistsException extends RabbitAdminException {

    public QueueAlreadyExistsException(String name) {
        super("La cola '" + name + "' ya existe");
    }
}

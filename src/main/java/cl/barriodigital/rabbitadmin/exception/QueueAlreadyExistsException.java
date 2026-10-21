package cl.barriodigital.rabbitadmin.exception;

public class QueueAlreadyExistsException extends RabbitAdminException {

    public QueueAlreadyExistsException(String name) {
        super("La cola '" + name + "' ya existe");
    }
}

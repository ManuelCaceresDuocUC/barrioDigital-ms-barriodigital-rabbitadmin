package cl.barriodigital.rabbitadmin.exception;

public class QueueNotFoundException extends RabbitAdminException {

    public QueueNotFoundException(String name) {
        super("La cola '" + name + "' no existe");
    }
}

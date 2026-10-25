package cl.barriodigital.rabbitadmin.exception;

/** Se pidió leer el contenido de una cola que no está habilitada para ello (solo las DLQ). */
public class InspectionNotAllowedException extends RabbitAdminException {

    public InspectionNotAllowedException(String name) {
        super("No se puede leer el contenido de la cola '" + name
                + "': solo está permitido en las colas de mensajes fallidos (DLQ)");
    }
}

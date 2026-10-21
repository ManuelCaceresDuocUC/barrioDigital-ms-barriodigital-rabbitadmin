package cl.barriodigital.rabbitadmin.exception;

/** Se intentó eliminar un elemento de la topología del sistema (colas q.cmd.*, exchanges cmd.* y amq.*). */
public class ProtectedResourceException extends RabbitAdminException {

    public ProtectedResourceException(String kind, String name) {
        super("'" + name + "' (" + kind + ") pertenece a la topología del sistema y no puede eliminarse");
    }
}

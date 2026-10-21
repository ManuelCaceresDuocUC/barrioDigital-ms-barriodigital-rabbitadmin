package cl.barriodigital.rabbitadmin.exception;

public class ExchangeAlreadyExistsException extends RabbitAdminException {

    public ExchangeAlreadyExistsException(String name) {
        super("El exchange '" + name + "' ya existe");
    }
}

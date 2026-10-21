package cl.barriodigital.rabbitadmin.exception;

public class ExchangeNotFoundException extends RabbitAdminException {

    public ExchangeNotFoundException(String name) {
        super("El exchange '" + name + "' no existe");
    }
}

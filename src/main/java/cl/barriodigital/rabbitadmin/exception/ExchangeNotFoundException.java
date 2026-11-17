package cl.barriodigital.rabbitadmin.exception;

/** QUÉ ES: excepción "el exchange no existe". Se traduce a HTTP 404. */
public class ExchangeNotFoundException extends RabbitAdminException {

    public ExchangeNotFoundException(String name) {
        super("El exchange '" + name + "' no existe");
    }
}

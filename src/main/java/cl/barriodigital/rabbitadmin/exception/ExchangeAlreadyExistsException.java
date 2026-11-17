package cl.barriodigital.rabbitadmin.exception;

/** QUÉ ES: excepción "ya existe un exchange con ese nombre". Se traduce a HTTP 409. */
public class ExchangeAlreadyExistsException extends RabbitAdminException {

    public ExchangeAlreadyExistsException(String name) {
        super("El exchange '" + name + "' ya existe");
    }
}

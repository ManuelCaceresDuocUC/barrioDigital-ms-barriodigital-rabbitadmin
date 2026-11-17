package cl.barriodigital.rabbitadmin.exception;

/** QUÉ ES: excepción "la estructura pedida (cola/exchange/binding o parámetro) no es válida". Se traduce a HTTP 400. */
public class InvalidTopologyException extends RabbitAdminException {

    public InvalidTopologyException(String message) {
        super(message);
    }
}

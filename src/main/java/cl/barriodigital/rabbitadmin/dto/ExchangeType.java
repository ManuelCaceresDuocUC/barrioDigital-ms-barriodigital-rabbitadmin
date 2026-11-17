package cl.barriodigital.rabbitadmin.dto;

/** QUÉ ES: tipos de exchange que RabbitMQ soporta. */
public enum ExchangeType {
    DIRECT, TOPIC, FANOUT, HEADERS;

    // Nombre que entiende RabbitMQ (en minúsculas).
    public String wireName() {
        return name().toLowerCase();
    }
}

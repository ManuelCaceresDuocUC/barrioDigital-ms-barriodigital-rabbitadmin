package cl.barriodigital.rabbitadmin.dto;

public enum ExchangeType {
    DIRECT, TOPIC, FANOUT, HEADERS;

    public String wireName() {
        return name().toLowerCase();
    }
}

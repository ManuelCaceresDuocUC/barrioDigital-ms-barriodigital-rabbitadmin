package cl.barriodigital.rabbitadmin.dto;

/** Binding de un exchange a una cola. Una routing key nula equivale a vacía. */
public record BindingRequest(String exchange, String queue, String routingKey) {
}

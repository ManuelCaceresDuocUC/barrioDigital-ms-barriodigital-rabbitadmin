package cl.barriodigital.rabbitadmin.dto;

public record BindingResponse(String exchange, String queue, String routingKey) {
}
